package com.nova.a12tester.domain

data class TestingAssignment(
    val assignmentId: String, // Deterministic "${appId}_${testerId}"
    val groupId: String,
    val appId: String,
    val appName: String,
    val packageName: String,
    val appIconUrl: String,
    val developerId: String,
    val developerName: String,
    val testerId: String,
    val testerDisplayName: String,
    val optInUrl: String,
    val googleGroupUrl: String,
    val testingInstructions: String,
    val requiredDays: Int = 14,
    val reportedDaysCount: Int = 0,
    val verifiedCheckInsCount: Int = 0,
    val groupJoinedConfirmed: Boolean = false,
    val optInConfirmed: Boolean = false,
    val installConfirmed: Boolean = false,
    val installationProblemReported: Boolean = false,
    val installationProblemDetails: String = "",
    val feedbackSubmitted: Boolean = false,
    val status: AssignmentStatus = AssignmentStatus.ASSIGNED,
    val rewardCoins: Int = 25,
    val rewardCredited: Boolean = false,
    val lastCheckInDateKey: String = "",
    val assignedAtEpochMs: Long = System.currentTimeMillis(),
    val lastCheckInEpochMs: Long = 0L,
    val completedAtEpochMs: Long = 0L
) {
    val remainingDays: Int get() = (requiredDays - reportedDaysCount).coerceAtLeast(0)
    val progressFraction: Float
        get() = if (requiredDays <= 0) 1f else (reportedDaysCount.toFloat() / requiredDays.toFloat()).coerceIn(0f, 1f)
}

enum class ParticipationStep(val label: String) {
    JOINED_GROUP("Joined Google Group (Self-Reported)"),
    OPTED_IN("Opted-In on Google Play (Self-Reported)"),
    INSTALLED_APP("Installed App from Play Store (Self-Reported)"),
    DAILY_CHECKIN("Daily Testing Check-In & Usage Note"),
    REPORTED_INSTALL_ISSUE("Reported Installation / Access Issue"),
    SUBMITTED_FEEDBACK("Submitted Structured Tester Feedback")
}

data class ParticipationRecord(
    val recordId: String,
    val assignmentId: String,
    val appId: String,
    val testerId: String,
    val dayNumber: Int,
    val dateKey: String,
    val verificationType: VerificationType,
    val step: ParticipationStep,
    val testerNotes: String,
    val screenshotUrl: String? = null, // Screenshot proof URL
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

enum class FeedbackCategory(val label: String) {
    BUG_REPORT("Bug Report"),
    UX_FEEDBACK("UI / UX Improvement"),
    INSTALL_PROBLEM("Installation / Opt-In Issue"),
    PERFORMANCE("Performance & Battery"),
    GENERAL("General Observation")
}

enum class FeedbackSeverity(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical")
}

data class TesterFeedback(
    val feedbackId: String,
    val assignmentId: String,
    val appId: String,
    val appName: String,
    val developerId: String,
    val testerId: String,
    val testerDisplayName: String,
    val category: FeedbackCategory,
    val severity: FeedbackSeverity,
    val summary: String,
    val reproductionSteps: String,
    val deviceInfo: String,
    val screenshotUrl: String? = null,
    val isMarkedHelpfulByDeveloper: Boolean = false,
    val bonusRewardCredited: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
