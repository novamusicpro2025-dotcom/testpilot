package com.nova.a12tester.domain

/**
 * Core Domain & Firestore Document Models for TestPilot – Closed Testing Community.
 *
 * Security & Compliance Design:
 * - Private user fields (such as raw email) are separated from public tester/developer summaries.
 * - Coin balances are server-authoritative and modified only through atomic ledger transactions.
 * - Activity tracking explicitly separates USER_REPORTED confirmations from
 *   SERVER_TIMESTAMPED_CHECKIN and FEEDBACK_ARTIFACT_VERIFIED records.
 */

enum class UserRole(val label: String) {
    TESTER("Tester"),
    DEVELOPER("Developer"),
    BOTH("Developer & Tester"),
    ADMIN("Administrator")
}

enum class AccountStatus(val label: String) {
    ACTIVE("Active"),
    SUSPENDED("Suspended"),
    DELETED("Deleted")
}

const val INACTIVITY_PURGE_DAYS = 90L
const val INACTIVITY_PURGE_MILLIS = INACTIVITY_PURGE_DAYS * 24L * 60L * 60L * 1000L // 3 months (90 days)

data class UserProfile(
    val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String = "",
    val role: UserRole = UserRole.BOTH,
    val coinBalance: Int = 0,
    val pendingRewardCoins: Int = 0,
    val reputationScore: Int = 100,
    val completedTestsCount: Int = 0,
    val submittedAppsCount: Int = 0,
    val androidSdkVersion: Int = 34,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val isAdminClaimVerified: Boolean = false,
    val privacyConsentAccepted: Boolean = false,
    val isPremium: Boolean = false,
    val premiumUntilEpochMs: Long = 0L,
    val premiumTier: String = "STANDARD", // "STANDARD", "PRO_1_MONTH", "VIP_3_MONTH"
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val lastActiveAtEpochMs: Long = System.currentTimeMillis(),
    val purgeAfterEpochMs: Long = System.currentTimeMillis() + INACTIVITY_PURGE_MILLIS
) {
    val isPremiumActive: Boolean
        get() = isPremium && premiumUntilEpochMs > System.currentTimeMillis()
    val isVipActive: Boolean
        get() = isPremiumActive && premiumTier == "VIP_3_MONTH"
    val premiumDaysRemaining: Int
        get() = if (isPremiumActive) {
            (((premiumUntilEpochMs - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).toInt()).coerceAtLeast(1)
        } else 0
}

enum class ModerationStatus(val label: String) {
    PENDING_REVIEW("Pending Moderation"),
    APPROVED("Approved & Active"),
    REJECTED("Rejected (Refunded)"),
    CANCELLED("Cancelled by Developer"),
    SUSPENDED("Suspended by Admin")
}

data class SubmittedApp(
    val appId: String,
    val developerId: String,
    val developerName: String,
    val appName: String,
    val packageName: String,
    val iconUrl: String,
    val shortDescription: String,
    val optInUrl: String,
    val googleGroupUrl: String,
    val privacyPolicyUrl: String,
    val minAndroidVersion: Int,
    val testingInstructions: String,
    val testingDurationDays: Int = 14,
    val submissionCostCoins: Int = 300,
    val rewardPerTesterCoins: Int = 25,
    val isFeaturedPremium: Boolean = false,
    val isVipTopPinned: Boolean = false,
    val moderationStatus: ModerationStatus = ModerationStatus.APPROVED,
    val moderationNotes: String = "",
    val targetTesterCount: Int = 12,
    val assignedTesterCount: Int = 0,
    val activeGroupId: String = "",
    val category: String = "Productivity",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val pinPriority: Int
        get() = if (isVipTopPinned) 2 else if (isFeaturedPremium) 1 else 0
}

enum class GroupStatus(val label: String) {
    FILLING("Recruiting Testers (Up to 12)"),
    ACTIVE("14-Day Closed Test Active"),
    COMPLETED("Testing Cycle Completed"),
    ARCHIVED("Archived")
}

data class TestingGroup(
    val groupId: String,
    val appId: String,
    val appName: String,
    val developerId: String,
    val developerName: String,
    val maxParticipants: Int = 12,
    val participantIds: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val minAndroidVersion: Int = 24,
    val status: GroupStatus = GroupStatus.FILLING,
    val startDateEpochMs: Long = System.currentTimeMillis(),
    val targetEndDateEpochMs: Long = System.currentTimeMillis() + 14L * 24L * 3600L * 1000L,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val currentCount: Int get() = participantIds.size
    val remainingSlots: Int get() = (maxParticipants - participantIds.size).coerceAtLeast(0)
}

enum class AssignmentStatus(val label: String) {
    ASSIGNED("Assigned - Setup Required"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed & Rewarded"),
    REPLACED("Replaced (Inactive)"),
    DISPUTED("Under Dispute Review"),
    ABANDONED("Abandoned")
}

enum class VerificationType(val badgeLabel: String, val explanation: String) {
    USER_REPORTED(
        badgeLabel = "User-Reported",
        explanation = "Self-reported by tester. Google Group membership and Play Store opt-in/install status cannot be independently verified via public Google APIs."
    ),
    SERVER_TIMESTAMPED_CHECKIN(
        badgeLabel = "Server-Timestamped Check-In",
        explanation = "Recorded via authenticated TestPilot server check-in with daily UTC rate limiting."
    ),
    FEEDBACK_ARTIFACT_VERIFIED(
        badgeLabel = "Verified Feedback Artifact",
        explanation = "Backed by structured bug/UX feedback submitted and stored in TestPilot."
    )
}
