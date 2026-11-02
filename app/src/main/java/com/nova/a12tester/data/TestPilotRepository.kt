package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single source of truth for TestPilot.
 *
 * Live Firebase backend: Firebase Auth (Google Sign-In), Firestore and Cloud Functions.
 * ALL coin/ledger/moderation/group operations are performed by callable Cloud Functions;
 * the client never writes coin balances directly.
 *
 * Security invariants:
 *  - coinBalance is only mutated inside an atomic ledger operation
 *  - every mutation uses a unique, idempotent transactionId (replay-safe)
 *  - Google Group membership / Play opt-in / install are USER_REPORTED only and are never
 *    presented as independently verified by Google APIs
 */
object TestPilotRepository {

    data class TestPilotState(
        val currentUser: UserProfile? = null,
        val users: List<UserProfile> = emptyList(),
        val apps: List<SubmittedApp> = emptyList(),
        val groups: List<TestingGroup> = emptyList(),
        val assignments: List<TestingAssignment> = emptyList(),
        val feedback: List<TesterFeedback> = emptyList(),
        val transactions: List<CoinTransaction> = emptyList(),
        val notifications: List<AppNotification> = emptyList(),
        val reports: List<AbuseReport> = emptyList(),
        val auditLogs: List<AdminAuditLog> = emptyList(),
        val moderationReviews: List<AppModerationReview> = emptyList(),
        val participationRecords: List<ParticipationRecord> = emptyList(),
        val usageSessions: List<DailyAppUsageSession> = emptyList(),
        val cohortTeams: List<TestingCohortTeam> = emptyList(),
        val rewardConfig: RewardConfig = RewardConfig(),
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val hasCompletedOnboarding: Boolean = false,
        val isLoading: Boolean = false,
        val chatMessages: List<ChatMessage> = emptyList(),
        val errorMessage: String? = null,
        val successMessage: String? = null
    ) {
        val isAdmin: Boolean get() = currentUser?.isAdminClaimVerified == true
    }

    internal val _state = MutableStateFlow(TestPilotState(apps = getInitialShowcaseApps()))
    val state: StateFlow<TestPilotState> = _state.asStateFlow()

    private val persistScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    init {
        persistScope.launch {
            state.collect { st ->
                st.currentUser?.let { LocalPersistence.saveUser(it) }
                if (st.transactions.isNotEmpty()) LocalPersistence.saveTransactions(st.transactions)
                if (st.apps.isNotEmpty()) LocalPersistence.saveApps(st.apps)
                if (st.cohortTeams.isNotEmpty()) LocalPersistence.saveCohortTeams(st.cohortTeams)
                if (st.usageSessions.isNotEmpty()) LocalPersistence.saveUsageSessions(st.usageSessions)
                if (st.assignments.isNotEmpty()) LocalPersistence.saveAssignments(st.assignments)
            }
        }
    }

    /**
     * Restores a previously signed-in user's session from Firestore after app restart.
     * Sets the user as currentUser and marks onboarding as complete.
     */
    fun restoreUserSession(user: UserProfile) {
        _state.update { st ->
            val users = if (st.users.any { it.userId == user.userId }) {
                st.users.map { if (it.userId == user.userId) user else it }
            } else {
                st.users + user
            }
            st.copy(
                currentUser = user,
                users = users,
                hasCompletedOnboarding = user.privacyConsentAccepted
            )
        }
    }

    /**
     * Mirrors the server-minted `admin` ID-token claim onto the current session.
     * Firestore rules trust ONLY the token claim; this field exists so the UI can
     * hide admin surfaces for everyone else. Applied on session restore/sign-in.
     */
    fun applyAdminClaim(hasAdminClaim: Boolean) {
        _state.update { st ->
            val user = st.currentUser ?: return@update st
            if (user.isAdminClaimVerified == hasAdminClaim) return@update st
            val updated = user.copy(isAdminClaimVerified = hasAdminClaim)
            st.copy(
                currentUser = updated,
                users = st.users.map { if (it.userId == user.userId) updated else it }
            )
        }
    }

    /**
     * Merges user-specific Firestore data into local state after a session restore.
     * User's own records take priority over previously loaded data.
     */
    fun mergeUserData(
        assignments: List<TestingAssignment>,
        transactions: List<CoinTransaction>,
        notifications: List<AppNotification>,
        usageSessions: List<DailyAppUsageSession>,
        cohortTeams: List<TestingCohortTeam>,
        apps: List<SubmittedApp> = emptyList()
    ) {
        _state.update { st ->
            // Merge apps: Firestore records override seed duplicates by appId
            val mergedApps = (st.apps + apps)
                .distinctBy { it.appId }

            // Merge assignments: Firestore records override seed duplicates by assignmentId
            val mergedAssignments = (st.assignments + assignments)
                .distinctBy { it.assignmentId }
                .sortedByDescending { it.assignedAtEpochMs }

            // Merge transactions: Firestore records override seed duplicates by transactionId
            val mergedTransactions = (st.transactions + transactions)
                .distinctBy { it.transactionId }
                .sortedByDescending { it.createdAtEpochMs }

            // Merge notifications: Firestore records override seed duplicates
            val mergedNotifications = (st.notifications + notifications)
                .distinctBy { it.notificationId }
                .sortedByDescending { it.createdAtEpochMs }

            // Merge usage sessions
            val mergedSessions = (st.usageSessions + usageSessions)
                .distinctBy { it.sessionId }

            // Merge teams
            val combinedTeams = (st.cohortTeams + cohortTeams)
                .groupBy { it.teamId }
                .map { (_, list) ->
                    list.maxByOrNull { it.members.size } ?: list.first()
                }
                .sortedBy { it.teamNumber }

            val mergedTeams = if (combinedTeams.isNotEmpty()) {
                ensureRecruitingTeamAvailable(combinedTeams)
            } else {
                listOf(getActiveOrRecruitingTeam())
            }

            st.copy(
                apps = mergedApps,
                assignments = mergedAssignments,
                transactions = mergedTransactions,
                notifications = mergedNotifications,
                usageSessions = mergedSessions,
                cohortTeams = mergedTeams
            )
        }
    }

    fun getInitialShowcaseApps(): List<SubmittedApp> {
        return emptyList()
    }

    fun consumeMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }
}
