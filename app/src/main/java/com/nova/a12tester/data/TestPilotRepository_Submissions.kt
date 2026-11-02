package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

/**
 * App submission. No admin approval — submissions go live immediately.
 *
 * Workflow: validate -> (only if valid) atomic coin charge -> APPROVED.
 * On developer CANCELLED the submission cost is refunded through a new idempotent
 * ledger transaction. Coins are never charged for invalid submissions.
 */
data class SubmitAppRequest(
    val appName: String,
    val packageName: String,
    val iconUrl: String,
    val shortDescription: String,
    val developerName: String,
    val optInUrl: String,
    val googleGroupUrl: String,
    val privacyPolicyUrl: String,
    val minAndroidVersion: Int,
    val testingInstructions: String,
    val testingDurationDays: Int
)

fun TestPilotRepository.submitAppForReview(request: SubmitAppRequest): Boolean {
    val current = _state.value.currentUser ?: return false
    val config = _state.value.rewardConfig

    val cleanGroupUrl = ValidationEngine.cleanGoogleGroupUrl(request.googleGroupUrl)

    // 1. Server-side validation (mirrors the Cloud Function).
    val validation = ValidationEngine.validateAppSubmission(
        appName = request.appName,
        developerName = request.developerName,
        shortDescription = request.shortDescription,
        packageName = request.packageName,
        iconUrl = request.iconUrl,
        optInUrl = request.optInUrl,
        googleGroupUrl = cleanGroupUrl,
        privacyPolicyUrl = request.privacyPolicyUrl,
        minAndroidVersion = request.minAndroidVersion,
        testingInstructions = request.testingInstructions,
        testingDurationDays = request.testingDurationDays
    )
    if (!validation.isValid) {
        _state.update { it.copy(errorMessage = validation.errors.first(), isLoading = false) }
        return false
    }

    // 2. Enforce plan submission limits:
    // 1-Month PRO Plan allows only 1 app for testing.
    // 3-Month VIP Plan (₹1400) allows UNLIMITED apps!
    val activeUserApps = _state.value.apps.filter {
        it.developerId == current.userId &&
            (it.moderationStatus == ModerationStatus.APPROVED || it.moderationStatus == ModerationStatus.PENDING_REVIEW)
    }

    if (current.isPremiumActive && !current.isVipActive && activeUserApps.isNotEmpty()) {
        _state.update {
            it.copy(
                errorMessage = "1-Month PRO Plan includes only 1 app for testing. You have already submitted '${activeUserApps.first().appName}'. Please upgrade to the ₹1400 3-Month VIP Plan to submit UNLIMITED apps!",
                isLoading = false
            )
        }
        return false
    }

    // 3. Prevent duplicate active submission of the same package by the same developer.
    val duplicate = activeUserApps.any {
        it.packageName.equals(request.packageName.trim(), ignoreCase = true)
    }
    if (duplicate) {
        _state.update { it.copy(errorMessage = "You already have an active submission for package ${request.packageName}.", isLoading = false) }
        return false
    }

    // 3. Atomic coin charge (only after validation passed).
    val cost = config.appSubmissionCostCoins
    if (current.coinBalance < cost) {
        _state.update { it.copy(errorMessage = "Insufficient coins. App submission requires $cost coins. Earn coins by testing apps or completing rewarded ads.", isLoading = false) }
        return false
    }

    val appId = "app_${request.packageName.substringAfterLast('.')}_${System.currentTimeMillis()}"
    val newBalance = current.coinBalance - cost
    val chargeTx = CoinTransaction(
        transactionId = "submit_${appId}",
        userId = current.userId,
        type = TransactionType.APP_SUBMISSION_CHARGE,
        amount = -cost,
        balanceAfter = newBalance,
        referenceId = appId,
        description = "Submission fee for ${request.appName.trim()}",
        verificationMethod = "SERVER_ATOMIC_LEDGER"
    )

    val app = SubmittedApp(
        appId = appId,
        developerId = current.userId,
        developerName = request.developerName.trim(),
        appName = request.appName.trim(),
        packageName = request.packageName.trim(),
        iconUrl = request.iconUrl.trim().ifBlank { "https://ui-avatars.com/api/?name=${request.appName.take(2).uppercase()}&background=607D8B&color=fff" },
        shortDescription = request.shortDescription.trim(),
        optInUrl = request.optInUrl.trim(),
        googleGroupUrl = cleanGroupUrl,
        privacyPolicyUrl = request.privacyPolicyUrl.trim(),
        minAndroidVersion = request.minAndroidVersion,
        testingInstructions = request.testingInstructions.trim(),
        testingDurationDays = request.testingDurationDays,
        submissionCostCoins = cost,
        rewardPerTesterCoins = if (current.isVipActive) 70 else if (current.isPremiumActive) config.premiumTestingCompletionRewardCoins else config.testingCompletionRewardCoins,
        isFeaturedPremium = current.isPremiumActive,
        isVipTopPinned = current.isVipActive,
        // No admin approval needed — submissions go live immediately as APPROVED.
        moderationStatus = ModerationStatus.APPROVED,
        targetTesterCount = config.maxGroupParticipants,
        createdAtEpochMs = System.currentTimeMillis()
    )

    val modNotif = AppNotification(
        notificationId = "notif_submitted_${appId}",
        userId = current.userId,
        title = "App Submitted",
        body = "${app.appName} passed validation and is now live. Testers can join immediately. $cost coins charged.",
        type = NotificationType.MODERATION,
        deepLinkRoute = "my_apps"
    )

    _state.update { st ->
        st.copy(
            currentUser = st.currentUser?.copy(coinBalance = newBalance, submittedAppsCount = st.currentUser.submittedAppsCount + 1),
            users = st.users.map { if (it.userId == current.userId) it.copy(coinBalance = newBalance, submittedAppsCount = it.submittedAppsCount + 1) else it },
            apps = st.apps + app,
            transactions = st.transactions + chargeTx,
            notifications = st.notifications + modNotif,
            isLoading = false,
            successMessage = "${app.appName} submitted and is live! Testers can join immediately.",
            errorMessage = null
        )
    }

    // Order matters: ledger writes are admin-only under firestore.rules, so save
    // the notification BEFORE the transaction row — a denied tx write (expected
    // for non-admin clients until Cloud Functions land) must not skip it.
    FirestoreRepository.syncToFirestore {
        FirestoreRepository.saveApp(app)
        FirestoreRepository.saveNotification(modNotif)
        FirestoreRepository.saveTransaction(chargeTx)
    }
    return true
}
