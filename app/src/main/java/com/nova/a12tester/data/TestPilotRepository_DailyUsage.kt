package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.TimeZone

const val MANDATORY_DAILY_TEST_SECONDS = 120 // 2 minutes per app
const val MANDATORY_DAILY_APPS_QUOTA = 10     // Minimum 10 apps tested daily

fun utcTodayKey(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

fun utcYesterdayKey(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.add(Calendar.DAY_OF_YEAR, -1)
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

/**
 * Record usage time (in seconds) for a tester's assigned app today.
 *
 * Requirements enforced:
 * 1. Each tester must open and use the installed test app for at least 2 minutes (120 seconds) daily.
 * 2. When 120 seconds is reached, daily check-in is automatically recorded with SERVER_TIMESTAMPED_CHECKIN.
 * 3. Daily quota (minimum 10 apps) progress is tracked. If 10 apps are tested today, developer apps are safe.
 */
fun TestPilotRepository.recordAppUsage(
    assignmentId: String,
    additionalSeconds: Int,
    dateKey: String = utcTodayKey()
): Boolean {
    if (additionalSeconds <= 0) return false
    val assignment = _state.value.assignments.firstOrNull { it.assignmentId == assignmentId } ?: return false
    val sessionId = "${assignmentId}_${dateKey}"
    val existing = _state.value.usageSessions.firstOrNull { it.sessionId == sessionId }

    val currentDuration = existing?.durationSeconds ?: 0
    val newDuration = currentDuration + additionalSeconds
    val targetMet = newDuration >= MANDATORY_DAILY_TEST_SECONDS
    val wasTargetMetBefore = existing?.isTargetMet == true

    val updatedSession = DailyAppUsageSession(
        sessionId = sessionId,
        assignmentId = assignmentId,
        appId = assignment.appId,
        appName = assignment.appName,
        packageName = assignment.packageName,
        testerId = assignment.testerId,
        dateKey = dateKey,
        durationSeconds = newDuration,
        requiredDurationSeconds = MANDATORY_DAILY_TEST_SECONDS,
        isTargetMet = targetMet,
        lastSessionStartEpochMs = System.currentTimeMillis(),
        updatedAtEpochMs = System.currentTimeMillis()
    )

    _state.update { st ->
        val sessions = if (existing != null) {
            st.usageSessions.map { if (it.sessionId == sessionId) updatedSession else it }
        } else {
            st.usageSessions + updatedSession
        }
        st.copy(usageSessions = sessions)
    }

    // When 2 minutes are reached for the first time today, automatically record the daily check-in
    if (targetMet && !wasTargetMetBefore) {
        recordDailyCheckIn(
            assignmentId = assignmentId,
            note = "Automated verified 2-minute testing session completed for ${assignment.appName}."
        )

        // Check if the user has reached the daily 10-app quota!
        val quotaStatus = getDailyTesterQuotaStatus(assignment.testerId, dateKey)
        if (quotaStatus.completedAppsCount == MANDATORY_DAILY_APPS_QUOTA) {
            addNotification(
                assignment.testerId,
                "🎉 Daily Testing Quota Complete (10/10 Apps)",
                "Great job! You have tested 10 apps for 2+ minutes today. Your submitted apps remain active and fully protected.",
                NotificationType.REWARD,
                "home"
            )
        }
    }

    FirestoreRepository.syncToFirestore {
        FirestoreRepository.saveUsageSession(updatedSession)
    }
    return true
}

/**
 * Get daily tester quota status.
 * Evaluates how many distinct apps the tester has used for at least 2 minutes today.
 */
fun TestPilotRepository.getDailyTesterQuotaStatus(
    testerId: String,
    dateKey: String = utcTodayKey()
): DailyTesterQuotaStatus {
    val completedSessions = _state.value.usageSessions.filter {
        it.testerId == testerId && it.dateKey == dateKey && it.isTargetMet
    }
    val completedCount = completedSessions.map { it.appId }.distinct().size

    val developerApps = _state.value.apps.filter { it.developerId == testerId }
    val appsAtRisk = developerApps.filter { it.moderationStatus == ModerationStatus.APPROVED }
    val removedApps = developerApps.filter {
        it.moderationStatus == ModerationStatus.SUSPENDED &&
            it.moderationNotes.contains("Daily testing quota not met", ignoreCase = true)
    }

    return DailyTesterQuotaStatus(
        testerId = testerId,
        dateKey = dateKey,
        requiredAppsCount = MANDATORY_DAILY_APPS_QUOTA,
        completedAppsCount = completedCount,
        isQuotaMet = completedCount >= MANDATORY_DAILY_APPS_QUOTA,
        developerAppsAtRisk = appsAtRisk,
        removedAppsDueToQuota = removedApps
    )
}

/**
 * Daily enforcement audit.
 * Rule: Testers who submit apps MUST test at least 10 apps daily (for 2+ minutes each).
 * If a developer failed to test minimum 10 apps on [auditDateKey], their submitted apps are REMOVED (SUSPENDED)
 * from active testing with clear rationale and notification.
 */
fun TestPilotRepository.enforceDailyTestingQuotaAudit(auditDateKey: String = utcYesterdayKey()): Int {
    var removedCount = 0
    val allDevelopersWithApprovedApps = _state.value.apps
        .filter { it.moderationStatus == ModerationStatus.APPROVED }
        .map { it.developerId }
        .distinct()

    for (devId in allDevelopersWithApprovedApps) {
        // VIP and PRO recharged developers as well as showcase apps have guaranteed listing and are exempt from quota suspensions
        val devUser = _state.value.users.firstOrNull { it.userId == devId }
        val isExempt = devUser?.isPremiumActive == true || devId.startsWith("dev_showcase")
        if (isExempt) continue

        val completedOnAuditDate = _state.value.usageSessions.count {
            it.testerId == devId && it.dateKey == auditDateKey && it.isTargetMet
        }

        if (completedOnAuditDate < MANDATORY_DAILY_APPS_QUOTA) {
            val devAppsToSuspend = _state.value.apps.filter {
                it.developerId == devId && it.moderationStatus == ModerationStatus.APPROVED && !it.isFeaturedPremium && !it.isVipTopPinned
            }

            for (app in devAppsToSuspend) {
                val reason = "Removed from testing: Daily testing quota not met on $auditDateKey. Tested only $completedOnAuditDate/$MANDATORY_DAILY_APPS_QUOTA apps (minimum 2 minutes each required). Test 10 apps today to reactivate."
                
                _state.update { st ->
                    st.copy(
                        apps = st.apps.map {
                            if (it.appId == app.appId) it.copy(
                                moderationStatus = ModerationStatus.SUSPENDED,
                                moderationNotes = reason,
                                updatedAtEpochMs = System.currentTimeMillis()
                            ) else it
                        },
                        auditLogs = st.auditLogs + AdminAuditLog(
                            logId = "audit_quota_remove_${app.appId}_${System.currentTimeMillis()}",
                            adminId = "system_quota_enforcer",
                            adminEmail = "system@testpilot.community",
                            action = "APP_REMOVED_INSUFFICIENT_DAILY_TESTING",
                            targetId = app.appId,
                            summary = "App '${app.appName}' removed: developer tested $completedOnAuditDate/10 apps on $auditDateKey.",
                            createdAtEpochMs = System.currentTimeMillis()
                        )
                    )
                }

                addNotification(
                    devId,
                    "⚠️ App Removed: Testing Quota Not Met",
                    "Your app '${app.appName}' was removed from active testing because you tested only $completedOnAuditDate/10 apps yesterday. Test 10 apps for 2 minutes each today to reactivate.",
                    NotificationType.MODERATION,
                    "my_apps"
                )
                removedCount++
            }
        }
    }
    return removedCount
}

/**
 * Reinstate an app if the developer has met today's 10-app daily testing quota!
 */
fun TestPilotRepository.reinstateAppIfDailyQuotaMet(appId: String): Boolean {
    val current = _state.value.currentUser ?: return false
    val app = _state.value.apps.firstOrNull { it.appId == appId && it.developerId == current.userId } ?: return false
    val quota = getDailyTesterQuotaStatus(current.userId, utcTodayKey())

    if (quota.completedAppsCount < MANDATORY_DAILY_APPS_QUOTA) {
        _state.update {
            it.copy(errorMessage = "Cannot reactivate yet: You have tested ${quota.completedAppsCount}/$MANDATORY_DAILY_APPS_QUOTA apps today. Complete ${quota.remainingAppsCount} more 2-minute tests.")
        }
        return false
    }

    _state.update { st ->
        st.copy(
            apps = st.apps.map {
                if (it.appId == appId) it.copy(
                    moderationStatus = ModerationStatus.APPROVED,
                    moderationNotes = "Reinstated: Daily testing quota requirement (10 apps) completed on ${utcTodayKey()}.",
                    updatedAtEpochMs = System.currentTimeMillis()
                ) else it
            },
            successMessage = "'${app.appName}' has been reactivated for testing! Keep up your daily 10-app testing."
        )
    }

    addNotification(
        current.userId,
        "✅ App Reactivated: ${app.appName}",
        "Your app is back in active testing. Continue testing 10 apps daily (2 minutes each) to keep it active.",
        NotificationType.MODERATION,
        "my_apps"
    )
    return true
}
