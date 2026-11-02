package com.nova.a12tester.domain

/**
 * Daily usage tracking for an installed test app.
 * Rule: Testers must open and actively use each installed test app for at least 2 minutes (120 seconds) daily.
 */
data class DailyAppUsageSession(
    val sessionId: String, // Deterministic: "${assignmentId}_${dateKey}"
    val assignmentId: String,
    val appId: String,
    val appName: String,
    val packageName: String,
    val testerId: String,
    val dateKey: String, // "YYYY-MM-DD"
    val durationSeconds: Int = 0, // Target: 120 seconds (2 minutes)
    val requiredDurationSeconds: Int = 120, // 2 minutes mandatory
    val isTargetMet: Boolean = false, // durationSeconds >= requiredDurationSeconds
    val lastSessionStartEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = (durationSeconds.toFloat() / requiredDurationSeconds.toFloat()).coerceIn(0f, 1f)
    val remainingSeconds: Int
        get() = (requiredDurationSeconds - durationSeconds).coerceAtLeast(0)
    val formattedDuration: String
        get() = "%02d:%02d".format(durationSeconds / 60, durationSeconds % 60)
    val formattedRequired: String
        get() = "%02d:%02d".format(requiredDurationSeconds / 60, requiredDurationSeconds % 60)
}

/**
 * Daily tester quota summary.
 * Mandatory Rule: Active testers who submit their own apps must test a minimum of 10 apps daily (2 minutes each).
 * If the minimum 10 apps quota is not met, the tester's own submitted app is REMOVED (suspended) from testing.
 */
data class DailyTesterQuotaStatus(
    val testerId: String,
    val dateKey: String,
    val requiredAppsCount: Int = 10,
    val completedAppsCount: Int = 0,
    val isQuotaMet: Boolean = false,
    val developerAppsAtRisk: List<SubmittedApp> = emptyList(),
    val removedAppsDueToQuota: List<SubmittedApp> = emptyList()
) {
    val progressFraction: Float
        get() = if (requiredAppsCount <= 0) 1f else (completedAppsCount.toFloat() / requiredAppsCount.toFloat()).coerceIn(0f, 1f)
    val remainingAppsCount: Int
        get() = (requiredAppsCount - completedAppsCount).coerceAtLeast(0)
}

/**
 * Closed Testing Reciprocal Cohort Team (Team 1, Team 2, Team 3...).
 * Process:
 *  - Each cohort team has a maximum of 13 developer users.
 *  - Joining a team requires 50 coins.
 *  - When 13 developers join, the team becomes ACTIVE for 14 days, and the next team (e.g. Team 2) is automatically created.
 *  - Each developer in the team must install and test the remaining 12 apps of the other members daily for 2 minutes for 14 days.
 */
data class TeamMember(
    val userId: String,
    val userDisplayName: String,
    val userEmail: String,
    val userPhotoUrl: String = "",
    val appId: String,
    val appName: String,
    val packageName: String,
    val appIconUrl: String,
    val optInUrl: String = "",
    val googleGroupUrl: String = "",
    val joinedAtEpochMs: Long = System.currentTimeMillis()
)

enum class CohortTeamStatus(val label: String) {
    RECRUITING("Recruiting Developers (Max 13)"),
    ACTIVE("14-Day Closed Testing Active"),
    COMPLETED("14-Day Cycle Completed")
}

data class TestingCohortTeam(
    val teamId: String,               // "team_1", "team_2", ...
    val teamNumber: Int,              // 1, 2, ...
    val teamName: String,             // "Team 1", "Team 2", ...
    val maxMembers: Int = 13,
    val members: List<TeamMember> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val status: CohortTeamStatus = CohortTeamStatus.RECRUITING,
    val joinCostCoins: Int = 200,
    val testingDurationDays: Int = 14,
    val dailyMinutesRequired: Int = 2,
    val startDateEpochMs: Long? = null,
    val endsAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val currentMembersCount: Int get() = members.size
    val remainingSlots: Int get() = (maxMembers - members.size).coerceAtLeast(0)
    val isFull: Boolean get() = members.size >= maxMembers
    val progressFraction: Float get() = (members.size.toFloat() / maxMembers.toFloat()).coerceIn(0f, 1f)
}

