package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

/**
 * Automatic testing group allocation.
 *
 * Eligibility rules enforced here (mirrored by the allocateTestingGroup Cloud Function):
 *  - app must be APPROVED
 *  - tester must not be the developer of the app
 *  - tester account must be ACTIVE and not SUSPENDED
 *  - tester device SDK must meet the app's minimum Android version
 *  - a tester is never assigned to the same app twice (deterministic assignmentId)
 *  - group is capped at [RewardConfig.maxGroupParticipants] (default 12)
 */
data class AllocationResult(val success: Boolean, val message: String)

fun TestPilotRepository.requestAutomaticGroupAllocation(appId: String): AllocationResult {
    val current = _state.value.currentUser ?: return AllocationResult(false, "You must be signed in.")
    val app = _state.value.apps.firstOrNull { it.appId == appId } ?: return AllocationResult(false, "App not found.")

    if (app.moderationStatus != ModerationStatus.APPROVED) {
        return AllocationResult(false, "This app has not completed moderation yet and is not open for testers.")
    }
    if (app.developerId == current.userId) {
        return AllocationResult(false, "You cannot join your own app's testing group.")
    }
    if (current.accountStatus != AccountStatus.ACTIVE) {
        return AllocationResult(false, "Your account is not eligible to join testing groups.")
    }
    if (current.androidSdkVersion < app.minAndroidVersion) {
        return AllocationResult(false, "This app requires Android API ${app.minAndroidVersion}; your device is API ${current.androidSdkVersion}.")
    }

    val assignmentId = "${app.appId}_${current.userId}"
    val alreadyAssigned = _state.value.assignments.any { it.assignmentId == assignmentId }
    if (alreadyAssigned) {
        return AllocationResult(false, "You are already assigned to ${app.appName}. Re-testing the same app is not allowed.")
    }

    val existingGroup = _state.value.groups.firstOrNull { it.appId == app.appId && it.status == GroupStatus.FILLING && it.remainingSlots > 0 }
    val maxParticipants = _state.value.rewardConfig.maxGroupParticipants

    val group: TestingGroup
    if (existingGroup != null) {
        group = existingGroup.copy(
            participantIds = existingGroup.participantIds + current.userId,
            participantNames = existingGroup.participantNames + current.displayName,
            status = if (existingGroup.participantIds.size + 1 >= maxParticipants) GroupStatus.ACTIVE else GroupStatus.FILLING
        )
    } else {
        group = TestingGroup(
            groupId = "grp_${app.appId}_${System.currentTimeMillis()}",
            appId = app.appId,
            appName = app.appName,
            developerId = app.developerId,
            developerName = app.developerName,
            maxParticipants = maxParticipants,
            participantIds = listOf(current.userId),
            participantNames = listOf(current.displayName),
            minAndroidVersion = app.minAndroidVersion,
            status = if (1 >= maxParticipants) GroupStatus.ACTIVE else GroupStatus.FILLING,
            startDateEpochMs = System.currentTimeMillis(),
            targetEndDateEpochMs = System.currentTimeMillis() + app.testingDurationDays.toLong() * 86_400_000L
        )
    }

    val assignment = TestingAssignment(
        assignmentId = assignmentId,
        groupId = group.groupId,
        appId = app.appId,
        appName = app.appName,
        packageName = app.packageName,
        appIconUrl = app.iconUrl,
        developerId = app.developerId,
        developerName = app.developerName,
        testerId = current.userId,
        testerDisplayName = current.displayName,
        optInUrl = app.optInUrl,
        googleGroupUrl = app.googleGroupUrl,
        testingInstructions = app.testingInstructions,
        requiredDays = app.testingDurationDays,
        rewardCoins = app.rewardPerTesterCoins,
        status = AssignmentStatus.ASSIGNED,
        assignedAtEpochMs = System.currentTimeMillis()
    )

    val assignmentNotif = AppNotification(
        notificationId = "notif_assign_${assignmentId}",
        userId = current.userId,
        title = "New Testing Assignment",
        body = "You have been assigned to ${app.appName}. Follow the opt-in and installation instructions.",
        type = NotificationType.ASSIGNMENT,
        deepLinkRoute = "active_assignments"
    )
    val devNotif = AppNotification(
        notificationId = "notif_assign_dev_${assignmentId}",
        userId = app.developerId,
        title = "New Tester Joined",
        body = "${current.displayName} joined the closed testing group for ${app.appName}.",
        type = NotificationType.ASSIGNMENT,
        deepLinkRoute = "my_apps"
    )

    _state.update { st ->
        st.copy(
            groups = if (existingGroup != null) st.groups.map { if (it.groupId == group.groupId) group else it } else st.groups + group,
            assignments = st.assignments + assignment,
            apps = st.apps.map { if (it.appId == app.appId) it.copy(assignedTesterCount = it.assignedTesterCount + 1, activeGroupId = group.groupId, updatedAtEpochMs = System.currentTimeMillis()) else it },
            notifications = st.notifications + assignmentNotif + devNotif,
            isLoading = false,
            successMessage = "You joined the ${app.appName} testing group (${group.currentCount}/${group.maxParticipants}).",
            errorMessage = null
        )
    }
    return AllocationResult(true, "Assigned to ${app.appName}.")
}
