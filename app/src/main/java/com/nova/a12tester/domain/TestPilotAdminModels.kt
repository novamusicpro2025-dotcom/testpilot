package com.nova.a12tester.domain

enum class TransactionType(val label: String) {
    WELCOME_BONUS("Welcome Bonus"),
    APP_SUBMISSION_CHARGE("App Submission Fee"),
    APP_REJECTION_REFUND("Submission Rejection Refund"),
    APP_CANCELLATION_REFUND("Submission Cancellation Refund"),
    TESTING_COMPLETION_REWARD("Testing Task Completion Reward"),
    DAILY_CHECKIN_MILESTONE("Testing Milestone Reward"),
    HELPFUL_FEEDBACK_BONUS("Helpful Feedback Bonus"),
    REWARDED_AD("Rewarded Ad Completion"),
    TEAM_JOIN_FEE("Team Cohort Join Fee"),
    REAL_MONEY_RECHARGE("₹500 1-Month PRO Plan"),
    PREMIUM_APP_INSTALL_REWARD("PRO App Install Reward (+50 Coins)"),
    ADMIN_ADJUSTMENT("Admin Ledger Adjustment")
}

data class CoinTransaction(
    val transactionId: String, // Unique idempotent key
    val userId: String,
    val type: TransactionType,
    val amount: Int,
    val balanceAfter: Int,
    val referenceId: String,
    val description: String,
    val verificationMethod: String = "SERVER_ATOMIC_LEDGER",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

enum class NotificationType(val label: String) {
    ASSIGNMENT("Testing Assignment"),
    MODERATION("App Moderation"),
    REWARD("Coins & Rewards"),
    FEEDBACK("Tester Feedback"),
    DISPUTE("Dispute / Admin Notice"),
    SYSTEM("System & Compliance")
}

data class AppNotification(
    val notificationId: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val deepLinkRoute: String = "home",
    val isRead: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

enum class ReportReason(val label: String) {
    BROKEN_OPT_IN_LINK("Broken Google Play Opt-In Link"),
    INVALID_GOOGLE_GROUP("Invalid or Locked Google Group URL"),
    MALWARE_OR_PHISHING("Suspected Malware, Phishing, or Unsafe App"),
    SPAM_OR_FAKE_APP("Spam, Placeholder, or Non-Functional App"),
    INCENTIVIZED_FAKE_REVIEW("Developer Asking for Fake Play Store Reviews"),
    POLICY_VIOLATION("Other Google Play / Community Policy Violation")
}

enum class ReportStatus(val label: String) {
    OPEN("Open - Needs Review"),
    INVESTIGATING("Under Investigation"),
    RESOLVED_REMOVED("Resolved - App Suspended"),
    DISMISSED("Dismissed")
}

data class AbuseReport(
    val reportId: String,
    val reporterId: String,
    val reporterName: String,
    val targetAppId: String,
    val targetAppName: String,
    val reason: ReportReason,
    val details: String,
    val status: ReportStatus = ReportStatus.OPEN,
    val adminResolutionNotes: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val resolvedAtEpochMs: Long = 0L
)

data class AppModerationReview(
    val reviewId: String,
    val appId: String,
    val appName: String,
    val reviewerAdminId: String,
    val decision: ModerationStatus,
    val urlValidationPassed: Boolean,
    val policyCheckPassed: Boolean,
    val reason: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class AdminAuditLog(
    val logId: String,
    val adminId: String,
    val adminEmail: String,
    val action: String,
    val targetId: String,
    val summary: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class RewardConfig(
    val welcomeBonusCoins: Int = 0,
    val appSubmissionCostCoins: Int = 300,
    val teamJoinCostCoins: Int = 200,
    val testingCompletionRewardCoins: Int = 25,
    val premiumTestingCompletionRewardCoins: Int = 50,
    val vipTestingCompletionRewardCoins: Int = 70,
    val helpfulFeedbackBonusCoins: Int = 10,
    val rewardedAdCoins: Int = 15,
    val dailyCheckInMilestoneCoins: Int = 5,
    val maxTeamMembers: Int = 13,
    val maxGroupParticipants: Int = 12,
    val defaultTestingDurationDays: Int = 14
)

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}
