package com.nova.a12tester.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nova.a12tester.domain.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firestore persistence layer for TestPilot.
 *
 * Collection structure:
 *   users/{uid}              → UserProfile fields
 *   apps/{appId}             → SubmittedApp fields
 *   assignments/{id}         → TestingAssignment fields
 *   transactions/{id}        → CoinTransaction fields
 *   groups/{groupId}         → TestingGroup fields
 *   cohortTeams/{teamId}     → TestingCohortTeam fields
 *   notifications/{id}       → AppNotification fields
 *   feedback/{id}            → TesterFeedback fields
 *   participationRecords/{id}→ ParticipationRecord fields
 *   usageSessions/{id}       → DailyAppUsageSession fields
 *
 * All writes use merge (SetOptions.merge()) so partial updates are safe.
 * Reads are one-time fetches called on app start for the signed-in user's data.
 */
object FirestoreRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val syncScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    /**
     * Executes a firestore write operation asynchronously in background.
     * Fails gracefully if offline.
     */
    fun syncToFirestore(action: suspend () -> Unit) {
        syncScope.launch {
            try {
                action()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreRepository", "Firestore background sync failed: ${e.message}")
            }
        }
    }

    // ─── User ───────────────────────────────────────────────────────────────

    suspend fun saveUser(user: UserProfile) {
        db.collection("users").document(user.userId)
            .set(user.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun loadUser(uid: String): UserProfile? {
        val doc = db.collection("users").document(uid).get().await()
        return if (doc.exists()) doc.toUserProfile() else null
    }

    /**
     * Completely erases a user's data from Firestore (Profile, Transactions, Assignments, Usage, Notifications, Apps, Cohort membership).
     * Used for 3-month inactivity account purge and GDPR compliance.
     */
    suspend fun deleteUserData(userId: String) {
        // 1. Delete user profile doc
        try {
            db.collection("users").document(userId).delete().await()
        } catch (e: Exception) {
            android.util.Log.d("FirestoreRepository", "deleteUserData user doc error: ${e.message}")
        }

        // 2. Delete user's coin transactions
        try {
            val txDocs = db.collection("transactions").whereEqualTo("userId", userId).get().await()
            for (doc in txDocs.documents) {
                doc.reference.delete().await()
            }
        } catch (_: Exception) {}

        // 3. Delete user's assignments
        try {
            val aDocs = db.collection("assignments").whereEqualTo("testerId", userId).get().await()
            for (doc in aDocs.documents) {
                doc.reference.delete().await()
            }
        } catch (_: Exception) {}

        // 4. Delete user's usage sessions
        try {
            val sDocs = db.collection("usageSessions").whereEqualTo("testerId", userId).get().await()
            for (doc in sDocs.documents) {
                doc.reference.delete().await()
            }
        } catch (_: Exception) {}

        // 5. Delete user's notifications
        try {
            val nDocs = db.collection("notifications").whereEqualTo("userId", userId).get().await()
            for (doc in nDocs.documents) {
                doc.reference.delete().await()
            }
        } catch (_: Exception) {}

        // 6. Delete user's submitted apps
        try {
            val appDocs = db.collection("apps").whereEqualTo("developerId", userId).get().await()
            for (doc in appDocs.documents) {
                doc.reference.delete().await()
            }
        } catch (_: Exception) {}

        // 7. Remove user from any cohort teams
        try {
            val teamDocs = db.collection("cohortTeams").whereArrayContains("memberIds", userId).get().await()
            for (doc in teamDocs.documents) {
                val team = doc.toTestingCohortTeam() ?: continue
                val updatedMembers = team.members.filterNot { it.userId == userId }
                val updatedIds = updatedMembers.map { it.userId }
                doc.reference.update(
                    mapOf(
                        "memberIds" to updatedIds,
                        "members" to updatedMembers.map { m ->
                            mapOf(
                                "userId" to m.userId, "userDisplayName" to m.userDisplayName,
                                "userEmail" to m.userEmail, "userPhotoUrl" to m.userPhotoUrl,
                                "appId" to m.appId, "appName" to m.appName,
                                "packageName" to m.packageName, "appIconUrl" to m.appIconUrl,
                                "optInUrl" to m.optInUrl, "googleGroupUrl" to m.googleGroupUrl,
                                "joinedAtEpochMs" to m.joinedAtEpochMs
                            )
                        }
                    )
                ).await()
            }
        } catch (_: Exception) {}
    }

    /**
     * Purges all users on the server who have been inactive for more than 90 days (3 months).
     */
    suspend fun purgeInactiveUsersFromServer(): Int {
        val cutoff = System.currentTimeMillis() - INACTIVITY_PURGE_MILLIS
        val staleUsers = db.collection("users")
            .whereLessThan("lastActiveAtEpochMs", cutoff)
            .get().await()
        var purgedCount = 0
        for (doc in staleUsers.documents) {
            val uid = doc.id
            deleteUserData(uid)
            purgedCount++
        }
        return purgedCount
    }

    // ─── Apps ────────────────────────────────────────────────────────────────

    suspend fun saveApp(app: SubmittedApp) {
        db.collection("apps").document(app.appId)
            .set(app.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun loadAllApps(): List<SubmittedApp> {
        return db.collection("apps").get().await()
            .documents.mapNotNull { it.toSubmittedApp() }
    }

    // ─── Assignments ─────────────────────────────────────────────────────────

    suspend fun saveAssignment(a: TestingAssignment) {
        db.collection("assignments").document(a.assignmentId)
            .set(a.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun loadAssignmentsForUser(uid: String): List<TestingAssignment> {
        return db.collection("assignments")
            .whereEqualTo("testerId", uid)
            .get().await()
            .documents.mapNotNull { it.toTestingAssignment() }
    }

    // ─── Transactions ────────────────────────────────────────────────────────

    suspend fun saveTransaction(tx: CoinTransaction) {
        db.collection("transactions").document(tx.transactionId)
            .set(tx.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun loadTransactionsForUser(uid: String): List<CoinTransaction> {
        return db.collection("transactions")
            .whereEqualTo("userId", uid)
            .get().await()
            .documents.mapNotNull { it.toCoinTransaction() }
    }

    // ─── Notifications ───────────────────────────────────────────────────────

    suspend fun saveNotification(n: AppNotification) {
        db.collection("notifications").document(n.notificationId)
            .set(n.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun loadNotificationsForUser(uid: String): List<AppNotification> {
        return db.collection("notifications")
            .whereEqualTo("userId", uid)
            .get().await()
            .documents.mapNotNull { it.toAppNotification() }
    }

    // ─── Groups ──────────────────────────────────────────────────────────────

    suspend fun saveGroup(g: TestingGroup) {
        db.collection("groups").document(g.groupId)
            .set(g.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun loadAllGroups(): List<TestingGroup> {
        return db.collection("groups").get().await()
            .documents.mapNotNull { it.toTestingGroup() }
    }

    // ─── Cohort Teams ────────────────────────────────────────────────────────

    suspend fun saveTeam(team: TestingCohortTeam) {
        db.collection("cohortTeams").document(team.teamId)
            .set(team.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun loadAllTeams(): List<TestingCohortTeam> {
        return db.collection("cohortTeams").get().await()
            .documents.mapNotNull { it.toTestingCohortTeam() }
    }

    // ─── Participation Records ────────────────────────────────────────────────

    suspend fun saveParticipationRecord(r: ParticipationRecord) {
        db.collection("participationRecords").document(r.recordId)
            .set(r.toMap(), SetOptions.merge())
            .await()
    }

    // ─── Usage Sessions ───────────────────────────────────────────────────────

    suspend fun saveUsageSession(s: DailyAppUsageSession) {
        db.collection("usageSessions").document(s.sessionId)
            .set(s.toMap(), SetOptions.merge())
            .await()
    }

    suspend fun loadUsageSessionsForUser(uid: String): List<DailyAppUsageSession> {
        return db.collection("usageSessions")
            .whereEqualTo("testerId", uid)
            .get().await()
            .documents.mapNotNull { it.toUsageSession() }
    }
}

// ─── Extension: Domain → Map ─────────────────────────────────────────────────

private fun UserProfile.toMap() = mapOf(
    "userId" to userId, "displayName" to displayName, "email" to email,
    "photoUrl" to photoUrl, "role" to role.name, "coinBalance" to coinBalance,
    "pendingRewardCoins" to pendingRewardCoins, "reputationScore" to reputationScore,
    "completedTestsCount" to completedTestsCount, "submittedAppsCount" to submittedAppsCount,
    "androidSdkVersion" to androidSdkVersion, "accountStatus" to accountStatus.name,
    "isAdminClaimVerified" to isAdminClaimVerified, "privacyConsentAccepted" to privacyConsentAccepted,
    "isPremium" to isPremium, "premiumUntilEpochMs" to premiumUntilEpochMs,
    "premiumTier" to premiumTier,
    "createdAtEpochMs" to createdAtEpochMs, "updatedAtEpochMs" to updatedAtEpochMs,
    "lastActiveAtEpochMs" to lastActiveAtEpochMs, "purgeAfterEpochMs" to purgeAfterEpochMs,
    "purgeAfterDate" to com.google.firebase.Timestamp(java.util.Date(purgeAfterEpochMs))
)

private fun SubmittedApp.toMap() = mapOf(
    "appId" to appId, "developerId" to developerId, "developerName" to developerName,
    "appName" to appName, "packageName" to packageName, "iconUrl" to iconUrl,
    "shortDescription" to shortDescription, "optInUrl" to optInUrl,
    "googleGroupUrl" to googleGroupUrl, "privacyPolicyUrl" to privacyPolicyUrl,
    "minAndroidVersion" to minAndroidVersion, "testingInstructions" to testingInstructions,
    "testingDurationDays" to testingDurationDays, "submissionCostCoins" to submissionCostCoins,
    "rewardPerTesterCoins" to rewardPerTesterCoins, "isFeaturedPremium" to isFeaturedPremium,
    "isVipTopPinned" to isVipTopPinned,
    "moderationStatus" to moderationStatus.name,
    "moderationNotes" to moderationNotes, "targetTesterCount" to targetTesterCount,
    "assignedTesterCount" to assignedTesterCount, "activeGroupId" to activeGroupId,
    "category" to category, "createdAtEpochMs" to createdAtEpochMs,
    "updatedAtEpochMs" to updatedAtEpochMs
)

private fun TestingAssignment.toMap() = mapOf(
    "assignmentId" to assignmentId, "groupId" to groupId, "appId" to appId,
    "appName" to appName, "packageName" to packageName, "appIconUrl" to appIconUrl,
    "developerId" to developerId, "developerName" to developerName,
    "testerId" to testerId, "testerDisplayName" to testerDisplayName,
    "optInUrl" to optInUrl, "googleGroupUrl" to googleGroupUrl,
    "testingInstructions" to testingInstructions, "requiredDays" to requiredDays,
    "reportedDaysCount" to reportedDaysCount, "verifiedCheckInsCount" to verifiedCheckInsCount,
    "groupJoinedConfirmed" to groupJoinedConfirmed, "optInConfirmed" to optInConfirmed,
    "installConfirmed" to installConfirmed, "rewardCoins" to rewardCoins,
    "rewardCredited" to rewardCredited, "status" to status.name,
    "assignedAtEpochMs" to assignedAtEpochMs, "completedAtEpochMs" to completedAtEpochMs,
    "lastCheckInDateKey" to lastCheckInDateKey, "lastCheckInEpochMs" to lastCheckInEpochMs,
    "installationProblemReported" to installationProblemReported,
    "installationProblemDetails" to installationProblemDetails
)

private fun CoinTransaction.toMap() = mapOf(
    "transactionId" to transactionId, "userId" to userId, "type" to type.name,
    "amount" to amount, "balanceAfter" to balanceAfter, "referenceId" to referenceId,
    "description" to description, "verificationMethod" to verificationMethod,
    "createdAtEpochMs" to createdAtEpochMs
)

private fun AppNotification.toMap() = mapOf(
    "notificationId" to notificationId, "userId" to userId, "title" to title,
    "body" to body, "type" to type.name, "isRead" to isRead,
    "deepLinkRoute" to deepLinkRoute, "createdAtEpochMs" to createdAtEpochMs
)

private fun TestingGroup.toMap() = mapOf(
    "groupId" to groupId, "appId" to appId, "appName" to appName,
    "developerId" to developerId, "developerName" to developerName,
    "maxParticipants" to maxParticipants, "participantIds" to participantIds,
    "participantNames" to participantNames, "minAndroidVersion" to minAndroidVersion,
    "status" to status.name, "startDateEpochMs" to startDateEpochMs,
    "targetEndDateEpochMs" to targetEndDateEpochMs
)

private fun TestingCohortTeam.toMap() = mapOf(
    "teamId" to teamId, "teamNumber" to teamNumber, "teamName" to teamName,
    "maxMembers" to maxMembers, "status" to status.name,
    "memberIds" to members.map { it.userId },
    "joinCostCoins" to joinCostCoins, "testingDurationDays" to testingDurationDays,
    "dailyMinutesRequired" to dailyMinutesRequired,
    "startDateEpochMs" to startDateEpochMs, "endsAtEpochMs" to endsAtEpochMs,
    "members" to members.map { m ->
        mapOf(
            "userId" to m.userId, "userDisplayName" to m.userDisplayName,
            "userEmail" to m.userEmail, "userPhotoUrl" to m.userPhotoUrl,
            "appId" to m.appId, "appName" to m.appName,
            "packageName" to m.packageName, "appIconUrl" to m.appIconUrl,
            "optInUrl" to m.optInUrl, "googleGroupUrl" to m.googleGroupUrl,
            "joinedAtEpochMs" to m.joinedAtEpochMs
        )
    }
)

private fun ParticipationRecord.toMap() = mapOf(
    "recordId" to recordId, "assignmentId" to assignmentId, "appId" to appId,
    "testerId" to testerId, "dayNumber" to dayNumber, "dateKey" to dateKey,
    "verificationType" to verificationType.name, "step" to step.name,
    "testerNotes" to testerNotes, "createdAtEpochMs" to createdAtEpochMs
)

private fun DailyAppUsageSession.toMap() = mapOf(
    "sessionId" to sessionId, "assignmentId" to assignmentId, "appId" to appId,
    "appName" to appName, "packageName" to packageName,
    "testerId" to testerId, "dateKey" to dateKey,
    "durationSeconds" to durationSeconds, "requiredDurationSeconds" to requiredDurationSeconds,
    "isTargetMet" to isTargetMet,
    "lastSessionStartEpochMs" to lastSessionStartEpochMs,
    "updatedAtEpochMs" to updatedAtEpochMs
)

// ─── Extension: DocumentSnapshot → Domain ────────────────────────────────────

private fun com.google.firebase.firestore.DocumentSnapshot.toUserProfile(): UserProfile? {
    return try {
        UserProfile(
            userId = getString("userId") ?: id,
            displayName = getString("displayName") ?: "",
            email = getString("email") ?: "",
            photoUrl = getString("photoUrl") ?: "",
            role = UserRole.valueOf(getString("role") ?: "BOTH"),
            coinBalance = getLong("coinBalance")?.toInt() ?: 0,
            pendingRewardCoins = getLong("pendingRewardCoins")?.toInt() ?: 0,
            reputationScore = getLong("reputationScore")?.toInt() ?: 100,
            completedTestsCount = getLong("completedTestsCount")?.toInt() ?: 0,
            submittedAppsCount = getLong("submittedAppsCount")?.toInt() ?: 0,
            androidSdkVersion = getLong("androidSdkVersion")?.toInt() ?: 34,
            accountStatus = AccountStatus.valueOf(getString("accountStatus") ?: "ACTIVE"),
            isAdminClaimVerified = getBoolean("isAdminClaimVerified") ?: false,
            privacyConsentAccepted = getBoolean("privacyConsentAccepted") ?: false,
            isPremium = getBoolean("isPremium") ?: false,
            premiumUntilEpochMs = getLong("premiumUntilEpochMs") ?: 0L,
            premiumTier = getString("premiumTier") ?: if (getBoolean("isPremium") == true) "PRO_1_MONTH" else "STANDARD",
            createdAtEpochMs = getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
            updatedAtEpochMs = getLong("updatedAtEpochMs") ?: System.currentTimeMillis(),
            lastActiveAtEpochMs = getLong("lastActiveAtEpochMs") ?: getLong("updatedAtEpochMs") ?: System.currentTimeMillis(),
            purgeAfterEpochMs = getLong("purgeAfterEpochMs") ?: (System.currentTimeMillis() + INACTIVITY_PURGE_MILLIS)
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toSubmittedApp(): SubmittedApp? {
    return try {
        SubmittedApp(
            appId = getString("appId") ?: id,
            developerId = getString("developerId") ?: "",
            developerName = getString("developerName") ?: "",
            appName = getString("appName") ?: "",
            packageName = getString("packageName") ?: "",
            iconUrl = getString("iconUrl") ?: "",
            shortDescription = getString("shortDescription") ?: "",
            optInUrl = getString("optInUrl") ?: "",
            googleGroupUrl = getString("googleGroupUrl") ?: "",
            privacyPolicyUrl = getString("privacyPolicyUrl") ?: "",
            minAndroidVersion = getLong("minAndroidVersion")?.toInt() ?: 26,
            testingInstructions = getString("testingInstructions") ?: "",
            testingDurationDays = getLong("testingDurationDays")?.toInt() ?: 14,
            submissionCostCoins = getLong("submissionCostCoins")?.toInt() ?: 300,
            rewardPerTesterCoins = getLong("rewardPerTesterCoins")?.toInt() ?: 25,
            isFeaturedPremium = getBoolean("isFeaturedPremium") ?: false,
            isVipTopPinned = getBoolean("isVipTopPinned") ?: false,
            moderationStatus = ModerationStatus.valueOf(getString("moderationStatus") ?: "APPROVED"),
            moderationNotes = getString("moderationNotes") ?: "",
            targetTesterCount = getLong("targetTesterCount")?.toInt() ?: 12,
            assignedTesterCount = getLong("assignedTesterCount")?.toInt() ?: 0,
            activeGroupId = getString("activeGroupId") ?: "",
            category = getString("category") ?: "",
            createdAtEpochMs = getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
            updatedAtEpochMs = getLong("updatedAtEpochMs") ?: System.currentTimeMillis()
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toTestingAssignment(): TestingAssignment? {
    return try {
        TestingAssignment(
            assignmentId = getString("assignmentId") ?: id,
            groupId = getString("groupId") ?: "",
            appId = getString("appId") ?: "",
            appName = getString("appName") ?: "",
            packageName = getString("packageName") ?: "",
            appIconUrl = getString("appIconUrl") ?: "",
            developerId = getString("developerId") ?: "",
            developerName = getString("developerName") ?: "",
            testerId = getString("testerId") ?: "",
            testerDisplayName = getString("testerDisplayName") ?: "",
            optInUrl = getString("optInUrl") ?: "",
            googleGroupUrl = getString("googleGroupUrl") ?: "",
            testingInstructions = getString("testingInstructions") ?: "",
            requiredDays = getLong("requiredDays")?.toInt() ?: 14,
            reportedDaysCount = getLong("reportedDaysCount")?.toInt() ?: 0,
            verifiedCheckInsCount = getLong("verifiedCheckInsCount")?.toInt() ?: 0,
            groupJoinedConfirmed = getBoolean("groupJoinedConfirmed") ?: false,
            optInConfirmed = getBoolean("optInConfirmed") ?: false,
            installConfirmed = getBoolean("installConfirmed") ?: false,
            rewardCoins = getLong("rewardCoins")?.toInt() ?: 25,
            rewardCredited = getBoolean("rewardCredited") ?: false,
            status = AssignmentStatus.valueOf(getString("status") ?: "ASSIGNED"),
            assignedAtEpochMs = getLong("assignedAtEpochMs") ?: System.currentTimeMillis(),
            completedAtEpochMs = getLong("completedAtEpochMs") ?: 0L,
            lastCheckInDateKey = getString("lastCheckInDateKey") ?: "",
            lastCheckInEpochMs = getLong("lastCheckInEpochMs") ?: 0L,
            installationProblemReported = getBoolean("installationProblemReported") ?: false,
            installationProblemDetails = getString("installationProblemDetails") ?: ""
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toCoinTransaction(): CoinTransaction? {
    return try {
        CoinTransaction(
            transactionId = getString("transactionId") ?: id,
            userId = getString("userId") ?: "",
            type = TransactionType.valueOf(getString("type") ?: "REWARDED_AD"),
            amount = getLong("amount")?.toInt() ?: 0,
            balanceAfter = getLong("balanceAfter")?.toInt() ?: 0,
            referenceId = getString("referenceId") ?: "",
            description = getString("description") ?: "",
            verificationMethod = getString("verificationMethod") ?: "",
            createdAtEpochMs = getLong("createdAtEpochMs") ?: System.currentTimeMillis()
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toAppNotification(): AppNotification? {
    return try {
        AppNotification(
            notificationId = getString("notificationId") ?: id,
            userId = getString("userId") ?: "",
            title = getString("title") ?: "",
            body = getString("body") ?: "",
            type = NotificationType.valueOf(getString("type") ?: "SYSTEM"),
            isRead = getBoolean("isRead") ?: false,
            deepLinkRoute = getString("deepLinkRoute") ?: "",
            createdAtEpochMs = getLong("createdAtEpochMs") ?: System.currentTimeMillis()
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toTestingGroup(): TestingGroup? {
    return try {
        @Suppress("UNCHECKED_CAST")
        TestingGroup(
            groupId = getString("groupId") ?: id,
            appId = getString("appId") ?: "",
            appName = getString("appName") ?: "",
            developerId = getString("developerId") ?: "",
            developerName = getString("developerName") ?: "",
            maxParticipants = getLong("maxParticipants")?.toInt() ?: 12,
            participantIds = (get("participantIds") as? List<String>) ?: emptyList(),
            participantNames = (get("participantNames") as? List<String>) ?: emptyList(),
            minAndroidVersion = getLong("minAndroidVersion")?.toInt() ?: 24,
            status = GroupStatus.valueOf(getString("status") ?: "FILLING"),
            startDateEpochMs = getLong("startDateEpochMs") ?: System.currentTimeMillis(),
            targetEndDateEpochMs = getLong("targetEndDateEpochMs") ?: System.currentTimeMillis()
        )
    } catch (e: Exception) { null }
}

@Suppress("UNCHECKED_CAST")
private fun com.google.firebase.firestore.DocumentSnapshot.toTestingCohortTeam(): TestingCohortTeam? {
    return try {
        val membersRaw = (get("members") as? List<Map<String, Any>>) ?: emptyList()
        val members = membersRaw.map { m ->
            TeamMember(
                userId = m["userId"] as? String ?: "",
                userDisplayName = m["userDisplayName"] as? String ?: "",
                userEmail = m["userEmail"] as? String ?: "",
                userPhotoUrl = m["userPhotoUrl"] as? String ?: "",
                appId = m["appId"] as? String ?: "",
                appName = m["appName"] as? String ?: "",
                packageName = m["packageName"] as? String ?: "",
                appIconUrl = m["appIconUrl"] as? String ?: "",
                optInUrl = m["optInUrl"] as? String ?: "",
                googleGroupUrl = m["googleGroupUrl"] as? String ?: "",
                joinedAtEpochMs = (m["joinedAtEpochMs"] as? Long) ?: System.currentTimeMillis()
            )
        }
        TestingCohortTeam(
            teamId = getString("teamId") ?: id,
            teamNumber = getLong("teamNumber")?.toInt() ?: 1,
            teamName = getString("teamName") ?: "Team 1",
            maxMembers = getLong("maxMembers")?.toInt() ?: 13,
            members = members,
            memberIds = members.map { it.userId },
            status = CohortTeamStatus.valueOf(getString("status") ?: "RECRUITING"),
            joinCostCoins = getLong("joinCostCoins")?.toInt() ?: 50,
            testingDurationDays = getLong("testingDurationDays")?.toInt() ?: 14,
            dailyMinutesRequired = getLong("dailyMinutesRequired")?.toInt() ?: 2,
            startDateEpochMs = getLong("startDateEpochMs") ?: 0L,
            endsAtEpochMs = getLong("endsAtEpochMs") ?: 0L
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toUsageSession(): DailyAppUsageSession? {
    return try {
        DailyAppUsageSession(
            sessionId = getString("sessionId") ?: id,
            assignmentId = getString("assignmentId") ?: "",
            appId = getString("appId") ?: "",
            appName = getString("appName") ?: "",
            packageName = getString("packageName") ?: "",
            testerId = getString("testerId") ?: "",
            dateKey = getString("dateKey") ?: "",
            durationSeconds = getLong("durationSeconds")?.toInt() ?: 0,
            requiredDurationSeconds = getLong("requiredDurationSeconds")?.toInt() ?: 120,
            isTargetMet = getBoolean("isTargetMet") ?: false,
            lastSessionStartEpochMs = getLong("lastSessionStartEpochMs") ?: 0L,
            updatedAtEpochMs = getLong("updatedAtEpochMs") ?: System.currentTimeMillis()
        )
    } catch (e: Exception) { null }
}
