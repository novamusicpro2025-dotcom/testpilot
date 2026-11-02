package com.nova.a12tester.data

import com.nova.a12tester.data.FirebaseAuthManager
import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

internal fun TestPilotRepository.addAuditLog(admin: UserProfile, action: String, targetId: String, summary: String) {
    val log = AdminAuditLog(
        logId = "audit_${System.currentTimeMillis()}_${admin.userId.hashCode()}",
        adminId = admin.userId,
        adminEmail = admin.email,
        action = action,
        targetId = targetId,
        summary = summary,
        createdAtEpochMs = System.currentTimeMillis()
    )
    _state.update { it.copy(auditLogs = it.auditLogs + log) }
}

suspend fun TestPilotRepository.approveApp(appId: String, notes: String = "") {
    val admin = _state.value.currentUser ?: return
    if (!FirebaseAuthManager.hasAdminClaim()) return
    val app = _state.value.apps.firstOrNull { it.appId == appId } ?: return
    val review = AppModerationReview(
        reviewId = "review_${appId}_${System.currentTimeMillis()}",
        appId = appId,
        appName = app.appName,
        reviewerAdminId = admin.userId,
        decision = ModerationStatus.APPROVED,
        urlValidationPassed = true,
        policyCheckPassed = true,
        reason = notes.ifBlank { "Validated Play Store opt-in URL, Google Group URL, and policy compliance." }
    )
    val approvedApp = app.copy(
        moderationStatus = ModerationStatus.APPROVED,
        moderationNotes = notes,
        updatedAtEpochMs = System.currentTimeMillis()
    )
    _state.update { st ->
        st.copy(
            apps = st.apps.map { if (it.appId == appId) approvedApp else it },
            moderationReviews = st.moderationReviews + review
        )
    }
    // Persist the decision — firestore.rules only accept moderation changes from
    // a request carrying the server-minted `admin` custom claim.
    FirestoreRepository.syncToFirestore { FirestoreRepository.saveApp(approvedApp) }
    addAuditLog(admin, "APPROVE_APP", appId, "Approved ${app.appName}")
    addNotification(app.developerId, "App Approved", "${app.appName} is approved and open for tester allocation.", NotificationType.MODERATION, "my_apps")
}

suspend fun TestPilotRepository.rejectApp(appId: String, reason: String) {
    val admin = _state.value.currentUser ?: return
    if (!FirebaseAuthManager.hasAdminClaim()) return
    val app = _state.value.apps.firstOrNull { it.appId == appId } ?: return
    val refundTxId = "reject_refund_${appId}"
    val alreadyRefunded = _state.value.transactions.any { it.transactionId == refundTxId }
    val dev = _state.value.users.firstOrNull { it.userId == app.developerId }
    val newBalance = if (!alreadyRefunded && dev != null) dev.coinBalance + app.submissionCostCoins else dev?.coinBalance ?: 0
    val refundTx = if (!alreadyRefunded && dev != null) {
        CoinTransaction(refundTxId, app.developerId, TransactionType.APP_REJECTION_REFUND, app.submissionCostCoins, newBalance, appId, "Refund for rejected submission ${app.appName}", "SERVER_ATOMIC_LEDGER")
    } else null

    val review = AppModerationReview(
        reviewId = "review_${appId}_${System.currentTimeMillis()}",
        appId = appId,
        appName = app.appName,
        reviewerAdminId = admin.userId,
        decision = ModerationStatus.REJECTED,
        urlValidationPassed = false,
        policyCheckPassed = false,
        reason = reason
    )
    _state.update { st ->
        st.copy(
            apps = st.apps.map { if (it.appId == appId) it.copy(moderationStatus = ModerationStatus.REJECTED, moderationNotes = reason, updatedAtEpochMs = System.currentTimeMillis()) else it },
            moderationReviews = st.moderationReviews + review,
            users = if (refundTx != null) st.users.map { if (it.userId == app.developerId) it.copy(coinBalance = newBalance, submittedAppsCount = (it.submittedAppsCount - 1).coerceAtLeast(0)) else it } else st.users,
            currentUser = if (st.currentUser?.userId == app.developerId && refundTx != null) st.currentUser.copy(coinBalance = newBalance, submittedAppsCount = (st.currentUser.submittedAppsCount - 1).coerceAtLeast(0)) else st.currentUser,
            transactions = if (refundTx != null) st.transactions + refundTx else st.transactions,
            groups = st.groups.map { g -> if (g.appId == appId) g.copy(status = GroupStatus.ARCHIVED) else g }
        )
    }
    addAuditLog(admin, "REJECT_APP_AND_REFUND", appId, "Rejected ${app.appName}: $reason")
    addNotification(app.developerId, "App Rejected (Refunded)", "${app.appName} was rejected. ${app.submissionCostCoins} coins refunded. Reason: $reason", NotificationType.MODERATION, "my_apps")
    // Persist the decision + refund (admin claim required by firestore.rules):
    // rejected status, the developer's restored balance, and the append-only
    // refund ledger row.
    FirestoreRepository.syncToFirestore {
        FirestoreRepository.saveApp(app.copy(moderationStatus = ModerationStatus.REJECTED, moderationNotes = reason, updatedAtEpochMs = System.currentTimeMillis()))
        if (refundTx != null && dev != null) {
            FirestoreRepository.saveUser(dev.copy(coinBalance = newBalance, submittedAppsCount = (dev.submittedAppsCount - 1).coerceAtLeast(0)))
            FirestoreRepository.saveTransaction(refundTx)
        }
    }
}

suspend fun TestPilotRepository.suspendUser(userId: String, reason: String) {
    val admin = _state.value.currentUser ?: return
    if (!FirebaseAuthManager.hasAdminClaim()) return
    val updatedUser = _state.value.users.firstOrNull { it.userId == userId }
        ?.copy(accountStatus = AccountStatus.SUSPENDED) ?: return
    _state.update { st -> st.copy(users = st.users.map { if (it.userId == userId) updatedUser else it }) }
    // Persist suspension (admin claim required by rules) — otherwise a restart
    // would silently restore the user's ACTIVE status.
    FirestoreRepository.syncToFirestore { FirestoreRepository.saveUser(updatedUser) }
    addAuditLog(admin, "SUSPEND_USER", userId, reason)
}

suspend fun TestPilotRepository.restoreUser(userId: String) {
    val admin = _state.value.currentUser ?: return
    if (!FirebaseAuthManager.hasAdminClaim()) return
    val updatedUser = _state.value.users.firstOrNull { it.userId == userId }
        ?.copy(accountStatus = AccountStatus.ACTIVE) ?: return
    _state.update { st -> st.copy(users = st.users.map { if (it.userId == userId) updatedUser else it }) }
    FirestoreRepository.syncToFirestore { FirestoreRepository.saveUser(updatedUser) }
    addAuditLog(admin, "RESTORE_USER", userId, "Restored account")
}

suspend fun TestPilotRepository.resolveReport(reportId: String, resolve: Boolean, notes: String) {
    val admin = _state.value.currentUser ?: return
    if (!FirebaseAuthManager.hasAdminClaim()) return
    val report = _state.value.reports.firstOrNull { it.reportId == reportId } ?: return
    val newStatus = if (resolve) ReportStatus.RESOLVED_REMOVED else ReportStatus.DISMISSED
    _state.update { st ->
        var apps = st.apps
        if (resolve) apps = apps.map { if (it.appId == report.targetAppId) it.copy(moderationStatus = ModerationStatus.SUSPENDED) else it }
        st.copy(
            reports = st.reports.map { if (it.reportId == reportId) it.copy(status = newStatus, adminResolutionNotes = notes, resolvedAtEpochMs = System.currentTimeMillis()) else it },
            apps = apps
        )
    }
    // Persist the app suspension (admin claim required by rules). The report
    // document itself has no Firestore code path yet — it stays in-memory.
    if (resolve) {
        _state.value.apps.firstOrNull { it.appId == report.targetAppId }
            ?.takeIf { it.moderationStatus == ModerationStatus.SUSPENDED }
            ?.let { suspended -> FirestoreRepository.syncToFirestore { FirestoreRepository.saveApp(suspended) } }
    }
    addAuditLog(admin, "RESOLVE_REPORT", reportId, "Resolved report against ${report.targetAppName}: $notes")
}
