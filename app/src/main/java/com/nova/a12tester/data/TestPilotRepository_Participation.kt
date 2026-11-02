package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

/**
 * Participation tracking.
 *
 * IMPORTANT (Google Play compliance):
 *  - groupJoined / optIn / install confirmations are USER_REPORTED. TestPilot does not and cannot
 *    independently verify Google Group membership or Google Play closed-testing eligibility through
 *    any supported public Google API, and never claims to.
 *  - DAILY_CHECKIN records are SERVER_TIMESTAMPED_CHECKIN entries, rate-limited to one per UTC day
 *    per assignment, so they cannot be replayed to inflate the count.
 *  - Feedback submissions are FEEDBACK_ARTIFACT_VERIFIED (a durable structured artifact exists).
 */
private fun utcDateKeyOf(epochMs: Long): String {
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = epochMs
    return "%04d-%02d-%02d".format(
        cal.get(java.util.Calendar.YEAR),
        cal.get(java.util.Calendar.MONTH) + 1,
        cal.get(java.util.Calendar.DAY_OF_MONTH)
    )
}

fun TestPilotRepository.confirmGroupJoined(assignmentId: String) =
    markStep(assignmentId, ParticipationStep.JOINED_GROUP, VerificationType.USER_REPORTED) { it.copy(groupJoinedConfirmed = true) }

fun TestPilotRepository.confirmOptedIn(assignmentId: String) =
    markStep(assignmentId, ParticipationStep.OPTED_IN, VerificationType.USER_REPORTED) { it.copy(optInConfirmed = true) }

fun TestPilotRepository.confirmInstalled(assignmentId: String): Boolean {
    val assignment = _state.value.assignments.firstOrNull { it.assignmentId == assignmentId }
    val alreadyConfirmed = assignment?.installConfirmed == true
    val result = markStep(assignmentId, ParticipationStep.INSTALLED_APP, VerificationType.USER_REPORTED) { it.copy(installConfirmed = true) }

    if (result && !alreadyConfirmed && assignment != null) {
        val app = _state.value.apps.firstOrNull { it.appId == assignment.appId }
        val isVip = app?.isVipTopPinned == true || assignment.rewardCoins >= 70
        val isPremium = app?.isFeaturedPremium == true || assignment.rewardCoins >= 50
        val rewardAmount = if (isVip) 70 else if (isPremium) 50 else (if ((app?.rewardPerTesterCoins ?: 0) > 0) app!!.rewardPerTesterCoins else 25)

        if (rewardAmount > 0) {
            creditUserInternal(
                userId = assignment.testerId,
                type = TransactionType.PREMIUM_APP_INSTALL_REWARD,
                amount = rewardAmount,
                transactionId = "install_reward_${assignmentId}",
                referenceId = assignmentId,
                description = "Installed ${if (isVip) "VIP Top Spotlight" else if (isPremium) "PRO" else "Community"} app: ${assignment.appName} (+$rewardAmount Coins bonus)"
            )
            addNotification(
                assignment.testerId,
                "${if (isVip) "👑 VIP Top App" else if (isPremium) "⭐ PRO App" else "📱 Community App"} Installed! (+$rewardAmount Coins) 🎉",
                "You earned $rewardAmount coins for installing ${assignment.appName}.",
                NotificationType.REWARD,
                "coin_wallet"
            )
        }
    }
    return result
}

fun TestPilotRepository.installAndRewardApp(appId: String): Int {
    val current = _state.value.currentUser ?: return 0
    val app = _state.value.apps.firstOrNull { it.appId == appId }
        ?: _state.value.cohortTeams.flatMap { it.members }.firstOrNull { it.appId == appId }?.let { m ->
            SubmittedApp(
                appId = m.appId,
                developerId = m.userId,
                developerName = m.userDisplayName,
                appName = m.appName,
                packageName = m.packageName,
                iconUrl = m.appIconUrl,
                shortDescription = "Team closed-testing peer app.",
                optInUrl = m.optInUrl,
                googleGroupUrl = m.googleGroupUrl,
                privacyPolicyUrl = "",
                minAndroidVersion = 24,
                testingInstructions = "Open and test daily for 2 minutes for 14 days.",
                testingDurationDays = 14,
                submissionCostCoins = 100,
                rewardPerTesterCoins = 25,
                moderationStatus = ModerationStatus.APPROVED,
                targetTesterCount = 12,
                assignedTesterCount = 1,
                createdAtEpochMs = m.joinedAtEpochMs
            ).also { newApp ->
                _state.update { st -> st.copy(apps = (st.apps + newApp).distinctBy { it.appId }) }
            }
        } ?: return 0

    val assignmentId = "${app.appId}_${current.userId}"

    // If assignment does not exist yet, allocate it automatically
    if (_state.value.assignments.none { it.assignmentId == assignmentId }) {
        requestAutomaticGroupAllocation(app.appId)
    }

    val isVip = app.isVipTopPinned || app.rewardPerTesterCoins >= 70
    val isPremium = app.isFeaturedPremium || app.rewardPerTesterCoins >= 50
    val rewardAmount = if (isVip) 70 else if (isPremium) 50 else (if (app.rewardPerTesterCoins > 0) app.rewardPerTesterCoins else 25)

    confirmInstalled(assignmentId)
    return rewardAmount
}

fun TestPilotRepository.reportInstallationProblem(assignmentId: String, details: String) =
    markStep(assignmentId, ParticipationStep.REPORTED_INSTALL_ISSUE, VerificationType.USER_REPORTED) {
        it.copy(installationProblemReported = true, installationProblemDetails = details, status = AssignmentStatus.DISPUTED)
    }

fun TestPilotRepository.recordDailyCheckIn(assignmentId: String, note: String, screenshotUrl: String? = null): Boolean {
    val now = System.currentTimeMillis()
    val todayKey = utcDateKeyOf(now)
    val assignment = _state.value.assignments.firstOrNull { it.assignmentId == assignmentId } ?: return false
    if (assignment.status != AssignmentStatus.ASSIGNED && assignment.status != AssignmentStatus.IN_PROGRESS) return false

    // Rate-limit: one server-timestamped check-in per UTC day.
    if (assignment.lastCheckInDateKey == todayKey) {
        _state.update { it.copy(errorMessage = "You have already recorded a check-in for today (UTC).") }
        return false
    }
    if (assignment.reportedDaysCount >= assignment.requiredDays) {
        _state.update { it.copy(errorMessage = "You have reached the required ${assignment.requiredDays} testing days.") }
        return false
    }

    val dayNumber = assignment.reportedDaysCount + 1
    val record = ParticipationRecord(
        recordId = "${assignmentId}_day_${dayNumber}",
        assignmentId = assignmentId,
        appId = assignment.appId,
        testerId = assignment.testerId,
        dayNumber = dayNumber,
        dateKey = todayKey,
        verificationType = VerificationType.SERVER_TIMESTAMPED_CHECKIN,
        step = ParticipationStep.DAILY_CHECKIN,
        testerNotes = note.trim(),
        screenshotUrl = screenshotUrl,
        createdAtEpochMs = now
    )

    val willComplete = (dayNumber >= assignment.requiredDays)
    _state.update { st ->
        st.copy(
            participationRecords = st.participationRecords + record,
            assignments = st.assignments.map { a ->
                if (a.assignmentId == assignmentId) {
                    a.copy(
                        reportedDaysCount = dayNumber,
                        verifiedCheckInsCount = a.verifiedCheckInsCount + 1,
                        lastCheckInDateKey = todayKey,
                        lastCheckInEpochMs = now,
                        status = AssignmentStatus.IN_PROGRESS
                    )
                } else a
            }
        )
    }

    // Milestone reward: every 7 server-verified check-ins credits a small bonus (idempotent).
    if (dayNumber % 7 == 0) {
        grantMilestoneReward(assignmentId, dayNumber)
    }
    return true
}

private fun TestPilotRepository.markStep(
    assignmentId: String,
    step: ParticipationStep,
    verification: VerificationType,
    transform: (TestingAssignment) -> TestingAssignment
): Boolean {
    val assignment = _state.value.assignments.firstOrNull { it.assignmentId == assignmentId } ?: return false
    val now = System.currentTimeMillis()
    val record = ParticipationRecord(
        recordId = "${assignmentId}_${step.name}_${now}",
        assignmentId = assignmentId,
        appId = assignment.appId,
        testerId = assignment.testerId,
        dayNumber = (assignment.reportedDaysCount + 1).coerceAtLeast(1),
        dateKey = utcDateKeyOf(now),
        verificationType = verification,
        step = step,
        testerNotes = "",
        createdAtEpochMs = now
    )
    _state.update { st ->
        st.copy(
            participationRecords = st.participationRecords + record,
            assignments = st.assignments.map {
                if (it.assignmentId == assignmentId) {
                    transform(it).copy(
                        status = if (it.status == AssignmentStatus.ASSIGNED) AssignmentStatus.IN_PROGRESS else it.status
                    )
                } else it
            }
        )
    }
    return true
}
