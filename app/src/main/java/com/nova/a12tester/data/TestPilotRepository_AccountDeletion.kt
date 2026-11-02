package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

/**
 * Account deletion. Removes the user profile and every associated community document
 * (submitted apps, groups, assignments, feedback, coin ledger, notifications, reports) and
 * writes a non-identifying audit entry. In LIVE_FIREBASE mode this is executed by the
 * `deleteAccountData` callable, which performs the same cascade server-side and deletes the
 * Firebase Auth record.
 */
fun TestPilotRepository.deleteCurrentUserAccountAndData() {
    val current = _state.value.currentUser ?: return
    val uid = current.userId
    val affectedAppIds = _state.value.apps.filter { it.developerId == uid }.map { it.appId }.toSet()

    val deletionAuditLog = AdminAuditLog(
        logId = "audit_delete_${uid}_${System.currentTimeMillis()}",
        adminId = "system",
        adminEmail = "system@testpilot.community",
        action = "USER_DELETION",
        targetId = uid,
        summary = "User deleted account and all associated data (privacy-preserving audit entry; no PII retained).",
        createdAtEpochMs = System.currentTimeMillis()
    )

    _state.update { st ->
        st.copy(
            users = st.users.filterNot { it.userId == uid },
            currentUser = null,
            hasCompletedOnboarding = false,
            apps = st.apps.filterNot { it.developerId == uid },
            groups = st.groups.map { g ->
                if (g.participantIds.contains(uid)) {
                    val keptIds = g.participantIds.filterNot { it == uid }
                    g.copy(participantIds = keptIds)
                } else g
            },
            assignments = st.assignments.filterNot { it.testerId == uid || affectedAppIds.contains(it.appId) },
            feedback = st.feedback.filterNot { it.testerId == uid || it.developerId == uid },
            transactions = st.transactions.filterNot { it.userId == uid },
            notifications = st.notifications.filterNot { it.userId == uid },
            reports = st.reports.filterNot { it.reporterId == uid },
            moderationReviews = st.moderationReviews.filterNot { affectedAppIds.contains(it.appId) },
            usageSessions = st.usageSessions.filterNot { it.testerId == uid },
            auditLogs = st.auditLogs + deletionAuditLog,
            isLoading = false,
            successMessage = "Account and associated data deleted.",
            errorMessage = null
        )
    }
}
