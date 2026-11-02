package com.nova.a12tester.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.nova.a12tester.domain.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust offline-first local persistence layer.
 * All data is encrypted at rest using EncryptedSharedPreferences with an AES-256-GCM master key
 * generated and stored in the Android Keystore (hardware-backed when available).
 * Ensures coins, user profile, transactions, apps, cohort teams, and testing sessions
 * are NEVER lost when the app is closed, killed, or restarted offline.
 */
object LocalPersistence {

    private const val PREFS_NAME = "testpilot_local_store_encrypted"
    private const val KEY_CURRENT_USER = "key_current_user"
    private const val KEY_TRANSACTIONS = "key_transactions"
    private const val KEY_APPS = "key_apps"
    private const val KEY_COHORT_TEAMS = "key_cohort_teams"
    private const val KEY_USAGE_SESSIONS = "key_usage_sessions"
    private const val KEY_ASSIGNMENTS = "key_assignments"

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            prefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context.applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    // ── User ──────────────────────────────────────────────────────────────────

    fun saveUser(user: UserProfile) {
        val p = prefs ?: return
        val json = userToJson(user).toString()
        p.edit().putString(KEY_CURRENT_USER, json).apply()
    }

    fun loadUser(): UserProfile? {
        val p = prefs ?: return null
        val raw = p.getString(KEY_CURRENT_USER, null) ?: return null
        return try {
            jsonToUser(JSONObject(raw))
        } catch (_: Exception) {
            null
        }
    }

    fun clearAll() {
        val p = prefs ?: return
        p.edit().clear().apply()
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    fun saveTransactions(transactions: List<CoinTransaction>) {
        val p = prefs ?: return
        val arr = JSONArray()
        transactions.forEach { arr.put(txToJson(it)) }
        p.edit().putString(KEY_TRANSACTIONS, arr.toString()).apply()
    }

    fun loadTransactions(): List<CoinTransaction> {
        val p = prefs ?: return emptyList()
        val raw = p.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CoinTransaction>()
            for (i in 0 until arr.length()) {
                list.add(jsonToTx(arr.getJSONObject(i)))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Apps ──────────────────────────────────────────────────────────────────

    fun saveApps(apps: List<SubmittedApp>) {
        val p = prefs ?: return
        val arr = JSONArray()
        apps.forEach { arr.put(appToJson(it)) }
        p.edit().putString(KEY_APPS, arr.toString()).apply()
    }

    fun loadApps(): List<SubmittedApp> {
        val p = prefs ?: return emptyList()
        val raw = p.getString(KEY_APPS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SubmittedApp>()
            for (i in 0 until arr.length()) {
                list.add(jsonToApp(arr.getJSONObject(i)))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Cohort Teams ──────────────────────────────────────────────────────────

    fun saveCohortTeams(teams: List<TestingCohortTeam>) {
        val p = prefs ?: return
        val arr = JSONArray()
        teams.forEach { arr.put(teamToJson(it)) }
        p.edit().putString(KEY_COHORT_TEAMS, arr.toString()).apply()
    }

    fun loadCohortTeams(): List<TestingCohortTeam> {
        val p = prefs ?: return emptyList()
        val raw = p.getString(KEY_COHORT_TEAMS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<TestingCohortTeam>()
            for (i in 0 until arr.length()) {
                list.add(jsonToTeam(arr.getJSONObject(i)))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Usage Sessions ────────────────────────────────────────────────────────

    fun saveUsageSessions(sessions: List<DailyAppUsageSession>) {
        val p = prefs ?: return
        val arr = JSONArray()
        sessions.forEach { arr.put(sessionToJson(it)) }
        p.edit().putString(KEY_USAGE_SESSIONS, arr.toString()).apply()
    }

    fun loadUsageSessions(): List<DailyAppUsageSession> {
        val p = prefs ?: return emptyList()
        val raw = p.getString(KEY_USAGE_SESSIONS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<DailyAppUsageSession>()
            for (i in 0 until arr.length()) {
                list.add(jsonToSession(arr.getJSONObject(i)))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Assignments ───────────────────────────────────────────────────────────

    fun saveAssignments(assignments: List<TestingAssignment>) {
        val p = prefs ?: return
        val arr = JSONArray()
        assignments.forEach { arr.put(assignmentToJson(it)) }
        p.edit().putString(KEY_ASSIGNMENTS, arr.toString()).apply()
    }

    fun loadAssignments(): List<TestingAssignment> {
        val p = prefs ?: return emptyList()
        val raw = p.getString(KEY_ASSIGNMENTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<TestingAssignment>()
            for (i in 0 until arr.length()) {
                list.add(jsonToAssignment(arr.getJSONObject(i)))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── JSON Converters ───────────────────────────────────────────────────────

    private fun userToJson(u: UserProfile) = JSONObject().apply {
        put("userId", u.userId)
        put("displayName", u.displayName)
        put("email", u.email)
        put("photoUrl", u.photoUrl)
        put("role", u.role.name)
        put("coinBalance", u.coinBalance)
        put("pendingRewardCoins", u.pendingRewardCoins)
        put("reputationScore", u.reputationScore)
        put("completedTestsCount", u.completedTestsCount)
        put("submittedAppsCount", u.submittedAppsCount)
        put("androidSdkVersion", u.androidSdkVersion)
        put("accountStatus", u.accountStatus.name)
        put("isAdminClaimVerified", u.isAdminClaimVerified)
        put("privacyConsentAccepted", u.privacyConsentAccepted)
        put("isPremium", u.isPremium)
        put("premiumUntilEpochMs", u.premiumUntilEpochMs)
        put("premiumTier", u.premiumTier)
        put("createdAtEpochMs", u.createdAtEpochMs)
        put("updatedAtEpochMs", u.updatedAtEpochMs)
        put("lastActiveAtEpochMs", u.lastActiveAtEpochMs)
        put("purgeAfterEpochMs", u.purgeAfterEpochMs)
    }

    private fun jsonToUser(json: JSONObject) = UserProfile(
        userId = json.optString("userId"),
        displayName = json.optString("displayName"),
        email = json.optString("email"),
        photoUrl = json.optString("photoUrl"),
        role = runCatching { UserRole.valueOf(json.optString("role", "BOTH")) }.getOrDefault(UserRole.BOTH),
        coinBalance = json.optInt("coinBalance", 0),
        pendingRewardCoins = json.optInt("pendingRewardCoins", 0),
        reputationScore = json.optInt("reputationScore", 100),
        completedTestsCount = json.optInt("completedTestsCount", 0),
        submittedAppsCount = json.optInt("submittedAppsCount", 0),
        androidSdkVersion = json.optInt("androidSdkVersion", 34),
        accountStatus = runCatching { AccountStatus.valueOf(json.optString("accountStatus", "ACTIVE")) }.getOrDefault(AccountStatus.ACTIVE),
        isAdminClaimVerified = json.optBoolean("isAdminClaimVerified", false),
        privacyConsentAccepted = json.optBoolean("privacyConsentAccepted", false),
        isPremium = json.optBoolean("isPremium", false),
        premiumUntilEpochMs = json.optLong("premiumUntilEpochMs", 0L),
        premiumTier = json.optString("premiumTier", if (json.optBoolean("isPremium", false)) "PRO_1_MONTH" else "STANDARD"),
        createdAtEpochMs = json.optLong("createdAtEpochMs", System.currentTimeMillis()),
        updatedAtEpochMs = json.optLong("updatedAtEpochMs", System.currentTimeMillis()),
        lastActiveAtEpochMs = json.optLong("lastActiveAtEpochMs", json.optLong("updatedAtEpochMs", System.currentTimeMillis())),
        purgeAfterEpochMs = json.optLong("purgeAfterEpochMs", System.currentTimeMillis() + INACTIVITY_PURGE_MILLIS)
    )

    private fun txToJson(t: CoinTransaction) = JSONObject().apply {
        put("transactionId", t.transactionId)
        put("userId", t.userId)
        put("type", t.type.name)
        put("amount", t.amount)
        put("balanceAfter", t.balanceAfter)
        put("referenceId", t.referenceId)
        put("description", t.description)
        put("verificationMethod", t.verificationMethod)
        put("createdAtEpochMs", t.createdAtEpochMs)
    }

    private fun jsonToTx(json: JSONObject) = CoinTransaction(
        transactionId = json.optString("transactionId"),
        userId = json.optString("userId"),
        type = runCatching { TransactionType.valueOf(json.optString("type")) }.getOrDefault(TransactionType.REWARDED_AD),
        amount = json.optInt("amount", 0),
        balanceAfter = json.optInt("balanceAfter", 0),
        referenceId = json.optString("referenceId"),
        description = json.optString("description"),
        verificationMethod = json.optString("verificationMethod", "SERVER_ATOMIC_LEDGER"),
        createdAtEpochMs = json.optLong("createdAtEpochMs", System.currentTimeMillis())
    )

    private fun appToJson(a: SubmittedApp) = JSONObject().apply {
        put("appId", a.appId)
        put("developerId", a.developerId)
        put("developerName", a.developerName)
        put("appName", a.appName)
        put("packageName", a.packageName)
        put("iconUrl", a.iconUrl)
        put("shortDescription", a.shortDescription)
        put("optInUrl", a.optInUrl)
        put("googleGroupUrl", a.googleGroupUrl)
        put("privacyPolicyUrl", a.privacyPolicyUrl)
        put("minAndroidVersion", a.minAndroidVersion)
        put("testingInstructions", a.testingInstructions)
        put("testingDurationDays", a.testingDurationDays)
        put("submissionCostCoins", a.submissionCostCoins)
        put("rewardPerTesterCoins", a.rewardPerTesterCoins)
        put("isFeaturedPremium", a.isFeaturedPremium)
        put("isVipTopPinned", a.isVipTopPinned)
        put("moderationStatus", a.moderationStatus.name)
        put("moderationNotes", a.moderationNotes)
        put("targetTesterCount", a.targetTesterCount)
        put("assignedTesterCount", a.assignedTesterCount)
        put("activeGroupId", a.activeGroupId)
        put("category", a.category)
        put("createdAtEpochMs", a.createdAtEpochMs)
        put("updatedAtEpochMs", a.updatedAtEpochMs)
    }

    private fun jsonToApp(json: JSONObject) = SubmittedApp(
        appId = json.optString("appId"),
        developerId = json.optString("developerId"),
        developerName = json.optString("developerName"),
        appName = json.optString("appName"),
        packageName = json.optString("packageName"),
        iconUrl = json.optString("iconUrl"),
        shortDescription = json.optString("shortDescription"),
        optInUrl = json.optString("optInUrl"),
        googleGroupUrl = json.optString("googleGroupUrl"),
        privacyPolicyUrl = json.optString("privacyPolicyUrl"),
        minAndroidVersion = json.optInt("minAndroidVersion", 24),
        testingInstructions = json.optString("testingInstructions"),
        testingDurationDays = json.optInt("testingDurationDays", 14),
        submissionCostCoins = json.optInt("submissionCostCoins", 300),
        rewardPerTesterCoins = json.optInt("rewardPerTesterCoins", 25),
        isFeaturedPremium = json.optBoolean("isFeaturedPremium", false),
        isVipTopPinned = json.optBoolean("isVipTopPinned", false),
        moderationStatus = runCatching { ModerationStatus.valueOf(json.optString("moderationStatus")) }.getOrDefault(ModerationStatus.APPROVED),
        moderationNotes = json.optString("moderationNotes"),
        targetTesterCount = json.optInt("targetTesterCount", 12),
        assignedTesterCount = json.optInt("assignedTesterCount", 0),
        activeGroupId = json.optString("activeGroupId"),
        category = json.optString("category", "Productivity"),
        createdAtEpochMs = json.optLong("createdAtEpochMs", System.currentTimeMillis()),
        updatedAtEpochMs = json.optLong("updatedAtEpochMs", System.currentTimeMillis())
    )

    private fun teamToJson(t: TestingCohortTeam) = JSONObject().apply {
        put("teamId", t.teamId)
        put("teamNumber", t.teamNumber)
        put("teamName", t.teamName)
        put("maxMembers", t.maxMembers)
        put("status", t.status.name)
        put("joinCostCoins", t.joinCostCoins)
        put("testingDurationDays", t.testingDurationDays)
        put("dailyMinutesRequired", t.dailyMinutesRequired)
        put("startDateEpochMs", t.startDateEpochMs)
        put("endsAtEpochMs", t.endsAtEpochMs)

        val memArr = JSONArray()
        t.members.forEach { m ->
            memArr.put(JSONObject().apply {
                put("userId", m.userId)
                put("userDisplayName", m.userDisplayName)
                put("userEmail", m.userEmail)
                put("userPhotoUrl", m.userPhotoUrl)
                put("appId", m.appId)
                put("appName", m.appName)
                put("packageName", m.packageName)
                put("appIconUrl", m.appIconUrl)
                put("optInUrl", m.optInUrl)
                put("googleGroupUrl", m.googleGroupUrl)
                put("joinedAtEpochMs", m.joinedAtEpochMs)
            })
        }
        put("members", memArr)
    }

    private fun jsonToTeam(json: JSONObject): TestingCohortTeam {
        val memArr = json.optJSONArray("members")
        val members = mutableListOf<TeamMember>()
        if (memArr != null) {
            for (i in 0 until memArr.length()) {
                val o = memArr.getJSONObject(i)
                members.add(
                    TeamMember(
                        userId = o.optString("userId"),
                        userDisplayName = o.optString("userDisplayName"),
                        userEmail = o.optString("userEmail"),
                        userPhotoUrl = o.optString("userPhotoUrl"),
                        appId = o.optString("appId"),
                        appName = o.optString("appName"),
                        packageName = o.optString("packageName"),
                        appIconUrl = o.optString("appIconUrl"),
                        optInUrl = o.optString("optInUrl"),
                        googleGroupUrl = o.optString("googleGroupUrl"),
                        joinedAtEpochMs = o.optLong("joinedAtEpochMs", System.currentTimeMillis())
                    )
                )
            }
        }
        return TestingCohortTeam(
            teamId = json.optString("teamId"),
            teamNumber = json.optInt("teamNumber", 1),
            teamName = json.optString("teamName"),
            maxMembers = json.optInt("maxMembers", 13),
            status = runCatching { CohortTeamStatus.valueOf(json.optString("status")) }.getOrDefault(CohortTeamStatus.RECRUITING),
            joinCostCoins = json.optInt("joinCostCoins", 50),
            testingDurationDays = json.optInt("testingDurationDays", 14),
            dailyMinutesRequired = json.optInt("dailyMinutesRequired", 2),
            startDateEpochMs = json.optLong("startDateEpochMs", 0L),
            endsAtEpochMs = json.optLong("endsAtEpochMs", 0L),
            members = members
        )
    }

    private fun sessionToJson(s: DailyAppUsageSession) = JSONObject().apply {
        put("sessionId", s.sessionId)
        put("assignmentId", s.assignmentId)
        put("appId", s.appId)
        put("appName", s.appName)
        put("packageName", s.packageName)
        put("testerId", s.testerId)
        put("dateKey", s.dateKey)
        put("durationSeconds", s.durationSeconds)
        put("requiredDurationSeconds", s.requiredDurationSeconds)
        put("isTargetMet", s.isTargetMet)
        put("lastSessionStartEpochMs", s.lastSessionStartEpochMs)
        put("updatedAtEpochMs", s.updatedAtEpochMs)
    }

    private fun jsonToSession(json: JSONObject) = DailyAppUsageSession(
        sessionId = json.optString("sessionId"),
        assignmentId = json.optString("assignmentId"),
        appId = json.optString("appId"),
        appName = json.optString("appName"),
        packageName = json.optString("packageName"),
        testerId = json.optString("testerId"),
        dateKey = json.optString("dateKey"),
        durationSeconds = json.optInt("durationSeconds", 0),
        requiredDurationSeconds = json.optInt("requiredDurationSeconds", 120),
        isTargetMet = json.optBoolean("isTargetMet", false),
        lastSessionStartEpochMs = json.optLong("lastSessionStartEpochMs", 0L),
        updatedAtEpochMs = json.optLong("updatedAtEpochMs", System.currentTimeMillis())
    )

    private fun assignmentToJson(a: TestingAssignment) = JSONObject().apply {
        put("assignmentId", a.assignmentId)
        put("groupId", a.groupId)
        put("appId", a.appId)
        put("appName", a.appName)
        put("packageName", a.packageName)
        put("appIconUrl", a.appIconUrl)
        put("developerId", a.developerId)
        put("developerName", a.developerName)
        put("testerId", a.testerId)
        put("testerDisplayName", a.testerDisplayName)
        put("optInUrl", a.optInUrl)
        put("googleGroupUrl", a.googleGroupUrl)
        put("testingInstructions", a.testingInstructions)
        put("requiredDays", a.requiredDays)
        put("reportedDaysCount", a.reportedDaysCount)
        put("verifiedCheckInsCount", a.verifiedCheckInsCount)
        put("groupJoinedConfirmed", a.groupJoinedConfirmed)
        put("optInConfirmed", a.optInConfirmed)
        put("installConfirmed", a.installConfirmed)
        put("installationProblemReported", a.installationProblemReported)
        put("installationProblemDetails", a.installationProblemDetails)
        put("feedbackSubmitted", a.feedbackSubmitted)
        put("status", a.status.name)
        put("rewardCoins", a.rewardCoins)
        put("rewardCredited", a.rewardCredited)
        put("lastCheckInDateKey", a.lastCheckInDateKey)
        put("assignedAtEpochMs", a.assignedAtEpochMs)
        put("lastCheckInEpochMs", a.lastCheckInEpochMs)
        put("completedAtEpochMs", a.completedAtEpochMs)
    }

    private fun jsonToAssignment(json: JSONObject) = TestingAssignment(
        assignmentId = json.optString("assignmentId"),
        groupId = json.optString("groupId"),
        appId = json.optString("appId"),
        appName = json.optString("appName"),
        packageName = json.optString("packageName"),
        appIconUrl = json.optString("appIconUrl"),
        developerId = json.optString("developerId"),
        developerName = json.optString("developerName"),
        testerId = json.optString("testerId"),
        testerDisplayName = json.optString("testerDisplayName"),
        optInUrl = json.optString("optInUrl"),
        googleGroupUrl = json.optString("googleGroupUrl"),
        testingInstructions = json.optString("testingInstructions"),
        requiredDays = json.optInt("requiredDays", 14),
        reportedDaysCount = json.optInt("reportedDaysCount", 0),
        verifiedCheckInsCount = json.optInt("verifiedCheckInsCount", 0),
        groupJoinedConfirmed = json.optBoolean("groupJoinedConfirmed", false),
        optInConfirmed = json.optBoolean("optInConfirmed", false),
        installConfirmed = json.optBoolean("installConfirmed", false),
        installationProblemReported = json.optBoolean("installationProblemReported", false),
        installationProblemDetails = json.optString("installationProblemDetails"),
        feedbackSubmitted = json.optBoolean("feedbackSubmitted", false),
        status = runCatching { AssignmentStatus.valueOf(json.optString("status")) }.getOrDefault(AssignmentStatus.ASSIGNED),
        rewardCoins = json.optInt("rewardCoins", 25),
        rewardCredited = json.optBoolean("rewardCredited", false),
        lastCheckInDateKey = json.optString("lastCheckInDateKey"),
        assignedAtEpochMs = json.optLong("assignedAtEpochMs", System.currentTimeMillis()),
        lastCheckInEpochMs = json.optLong("lastCheckInEpochMs", 0L),
        completedAtEpochMs = json.optLong("completedAtEpochMs", 0L)
    )
}
