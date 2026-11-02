package com.nova.a12tester.data

import com.nova.a12tester.data.FirebaseAuthManager
import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

fun TestPilotRepository.setThemeMode(mode: ThemeMode) = _state.update { it.copy(themeMode = mode) }

fun TestPilotRepository.setOnboardingComplete(complete: Boolean) = _state.update { it.copy(hasCompletedOnboarding = complete) }

/**
 * Admin-only: replace an inactive tester with an eligible replacement from the group waitlist
 * (here: next eligible active user not already assigned to the app).
 */
suspend fun TestPilotRepository.replaceInactiveTester(assignmentId: String): Boolean {
    val admin = _state.value.currentUser ?: return false
    if (!FirebaseAuthManager.hasAdminClaim()) return false
    val assignment = _state.value.assignments.firstOrNull { it.assignmentId == assignmentId } ?: return false
    val app = _state.value.apps.firstOrNull { it.appId == assignment.appId } ?: return false

    val candidate = _state.value.users.firstOrNull { u ->
        u.userId != assignment.testerId &&
            u.accountStatus == AccountStatus.ACTIVE &&
            u.userId != app.developerId &&
            u.androidSdkVersion >= app.minAndroidVersion &&
            _state.value.assignments.none { it.assignmentId == "${app.appId}_${u.userId}" }
    } ?: run {
        _state.update { it.copy(errorMessage = "No eligible replacement tester is available.") }
        return false
    }

    val newAssignmentId = "${app.appId}_${candidate.userId}"
    val replacement = assignment.copy(
        assignmentId = newAssignmentId,
        testerId = candidate.userId,
        testerDisplayName = candidate.displayName,
        reportedDaysCount = 0,
        verifiedCheckInsCount = 0,
        groupJoinedConfirmed = false,
        optInConfirmed = false,
        installConfirmed = false,
        feedbackSubmitted = false,
        status = AssignmentStatus.ASSIGNED,
        rewardCredited = false,
        lastCheckInDateKey = "",
        assignedAtEpochMs = System.currentTimeMillis()
    )
    _state.update { st ->
        st.copy(
            assignments = st.assignments.map { if (it.assignmentId == assignmentId) it.copy(status = AssignmentStatus.REPLACED) else it } + replacement,
            groups = st.groups.map { g ->
                if (g.groupId == assignment.groupId) g.copy(participantIds = g.participantIds.map { if (it == assignment.testerId) candidate.userId else it }) else g
            }
        )
    }
    addAuditLog(admin, "REPLACE_INACTIVE_TESTER", newAssignmentId, "Replaced ${assignment.testerDisplayName} with ${candidate.displayName} for ${app.appName}")
    addNotification(candidate.userId, "New Testing Assignment", "You were assigned to ${app.appName} by the moderation team.", NotificationType.ASSIGNMENT, "active_assignments")
    return true
}
