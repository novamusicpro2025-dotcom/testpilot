package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

fun TestPilotRepository.cancelAppSubmission(appId: String) {
    val current = _state.value.currentUser ?: return
    val app = _state.value.apps.firstOrNull { it.appId == appId && it.developerId == current.userId } ?: return
    if (app.moderationStatus != ModerationStatus.PENDING_REVIEW && app.moderationStatus != ModerationStatus.APPROVED) return

    val refundTxId = "cancel_refund_${appId}"
    val alreadyRefunded = _state.value.transactions.any { it.transactionId == refundTxId }
    val newBalance = current.coinBalance + if (alreadyRefunded) 0 else app.submissionCostCoins
    val refundTx = if (!alreadyRefunded) {
        CoinTransaction(
            transactionId = refundTxId,
            userId = current.userId,
            type = TransactionType.APP_CANCELLATION_REFUND,
            amount = app.submissionCostCoins,
            balanceAfter = newBalance,
            referenceId = appId,
            description = "Refund for cancelled submission ${app.appName}",
            verificationMethod = "SERVER_ATOMIC_LEDGER"
        )
    } else null

    _state.update { st ->
        st.copy(
            apps = st.apps.map { if (it.appId == appId) it.copy(moderationStatus = ModerationStatus.CANCELLED, updatedAtEpochMs = System.currentTimeMillis()) else it },
            users = st.users.map { if (it.userId == current.userId) it.copy(coinBalance = newBalance, submittedAppsCount = (it.submittedAppsCount - 1).coerceAtLeast(0)) else it },
            currentUser = st.currentUser?.copy(coinBalance = newBalance, submittedAppsCount = (st.currentUser.submittedAppsCount - 1).coerceAtLeast(0)),
            transactions = if (refundTx != null) st.transactions + refundTx else st.transactions,
            groups = st.groups.map { g -> if (g.appId == appId) g.copy(status = GroupStatus.ARCHIVED) else g },
            assignments = st.assignments.map { a -> if (a.appId == appId && a.status != AssignmentStatus.COMPLETED) a.copy(status = AssignmentStatus.DISPUTED) else a },
            isLoading = false,
            successMessage = "Submission cancelled and ${app.submissionCostCoins} coins refunded.",
            errorMessage = null
        )
    }
}
