package com.nova.a12tester.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import java.util.Calendar

object UsageStatsHelper {

    fun isUsageAccessPermissionGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Accurately calculates total foreground time (in seconds) for a package today (from 00:00 local time to now).
     */
    fun getAppForegroundUsageTodaySeconds(context: Context, packageName: String): Int {
        if (!isUsageAccessPermissionGranted(context)) return 0
        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        // 1. First attempt: aggregated stats (recommended on modern Android versions)
        try {
            val aggregated = usageManager.queryAndAggregateUsageStats(startTime, endTime)
            val usage = aggregated[packageName]
            if (usage != null && usage.totalTimeInForeground > 0) {
                return (usage.totalTimeInForeground / 1000L).toInt()
            }
        } catch (_: Exception) {}

        // 2. Fallback: queryUsageStats and sum total time in foreground across all intervals
        val stats = usageManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return 0

        val matchingStats = stats.filter { it.packageName == packageName }
        if (matchingStats.isNotEmpty()) {
            val totalMs = matchingStats.sumOf { it.totalTimeInForeground }
            return (totalMs / 1000L).toInt()
        }

        return 0
    }

    /**
     * Synchronize real Android OS foreground usage with TestPilot records for all active assignments
     * and peer apps in active cohort teams.
     */
    fun syncRealDeviceUsage(context: Context) {
        if (!isUsageAccessPermissionGranted(context)) return
        val currentUserId = TestPilotRepository.state.value.currentUser?.userId ?: return
        val todayKey = utcTodayKey()

        // 1. Sync all active user assignments
        val activeAssignments = TestPilotRepository.state.value.assignments.filter { it.testerId == currentUserId }
        for (a in activeAssignments) {
            val realSeconds = getAppForegroundUsageTodaySeconds(context, a.packageName)
            if (realSeconds > 0) {
                val currentSession = TestPilotRepository.state.value.usageSessions.firstOrNull {
                    it.assignmentId == a.assignmentId && it.dateKey == todayKey
                }
                val currentSeconds = currentSession?.durationSeconds ?: 0
                if (realSeconds > currentSeconds) {
                    TestPilotRepository.recordAppUsage(a.assignmentId, realSeconds - currentSeconds)
                }
            }
        }

        // 2. Sync all peer apps from user's cohort teams
        val userTeams = TestPilotRepository.state.value.cohortTeams.filter { it.memberIds.contains(currentUserId) }
        for (team in userTeams) {
            val peerMembers = team.members.filter { it.userId != currentUserId }
            for (peer in peerMembers) {
                val aId = "${peer.appId}_${currentUserId}"
                // Ensure assignment exists so recordAppUsage works reliably
                TestPilotRepository.ensurePeerAssignment(peer, currentUserId, team.teamId)

                val realSeconds = getAppForegroundUsageTodaySeconds(context, peer.packageName)
                if (realSeconds > 0) {
                    val currentSession = TestPilotRepository.state.value.usageSessions.firstOrNull {
                        it.assignmentId == aId && it.dateKey == todayKey
                    }
                    val currentSeconds = currentSession?.durationSeconds ?: 0
                    if (realSeconds > currentSeconds) {
                        TestPilotRepository.recordAppUsage(aId, realSeconds - currentSeconds)
                    }
                }
            }
        }
    }
}
