package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

/**
 * Handles the ₹500 (1-Month PRO) and ₹1400 (3-Month VIP) Membership Recharges.
 *
 * Rules for ₹500 1-Month PRO:
 *  - 1 Month (30 days) validity with ad-free experience.
 *  - Instant +500 bonus coins credited to ledger.
 *  - Pinned on Home Screen.
 *  - Testers who test this app are rewarded 50 coins (instead of 25).
 *
 * Rules for ₹1400 3-Month VIP:
 *  - 3 Months (90 days) validity with ad-free experience.
 *  - Instant +1500 bonus coins credited to ledger.
 *  - Guaranteed #1 Top Pinned Spotlight on Home Screen (appears above all other apps).
 *  - Testers who install/test this app are rewarded 70 coins.
 *  - 100% Guaranteed Google Play Store Production Access Protection Grant.
 */
fun TestPilotRepository.rechargePremium500Plan(paymentReference: String, serverVerified: Boolean = false): Boolean {
    // Real-money recharge requires server-side payment verification.
    // Client-supplied payment references without server verification are rejected.
    if (!serverVerified) {
        _state.update { it.copy(errorMessage = "Payment must be verified by server before premium activation.") }
        return false
    }
    val current = _state.value.currentUser ?: return false
    val now = System.currentTimeMillis()
    val validityDuration = 30L * 24L * 60L * 60L * 1000L // 30 days
    val currentExpiry = if (current.isPremiumActive) current.premiumUntilEpochMs else now
    val newExpiry = currentExpiry + validityDuration
    val bonusCoins = 500
    val newBalance = current.coinBalance + bonusCoins

    val tx = CoinTransaction(
        transactionId = "vip_recharge_${current.userId}_${now}",
        userId = current.userId,
        type = TransactionType.REAL_MONEY_RECHARGE,
        amount = bonusCoins,
        balanceAfter = newBalance,
        referenceId = paymentReference,
        description = "₹500 1-Month PRO Developer Plan (Ad-Free, Home Spotlight, +500 Coins)",
        verificationMethod = "SERVER_ATOMIC_LEDGER",
        createdAtEpochMs = now
    )

    val updatedUser = current.copy(
        isPremium = true,
        premiumUntilEpochMs = newExpiry,
        premiumTier = if (current.isVipActive) "VIP_3_MONTH" else "PRO_1_MONTH",
        coinBalance = newBalance,
        updatedAtEpochMs = now
    )

    val notif = AppNotification(
        notificationId = "notif_vip_${current.userId}_${now}",
        userId = current.userId,
        title = "👑 1-Month PRO Activated (30 Days) 🛡️",
        body = "Thank you for recharging ₹500! 100% Guaranteed Google Play Store Production Access Protection Grant included for 1 app. You have 30 days of ad-free testing, 500 bonus coins, your app is pinned on the Home screen, and testers get 50 coins.",
        type = NotificationType.REWARD,
        deepLinkRoute = "coin_wallet",
        createdAtEpochMs = now
    )

    // Upgrade existing developer apps to PRO Featured status (+50 Coins)
    // We DO NOT create dummy apps anymore. If they have none, they just get the coins and premium status.
    val updatedApps = _state.value.apps.map { app ->
        if (app.developerId == current.userId) {
            app.copy(
                moderationStatus = ModerationStatus.APPROVED,
                isFeaturedPremium = true,
                rewardPerTesterCoins = if (app.isVipTopPinned) 70 else 50,
                updatedAtEpochMs = now
            )
        } else app
    }

    _state.update { st ->
        st.copy(
            currentUser = updatedUser,
            users = st.users.map { if (it.userId == current.userId) updatedUser else it },
            transactions = st.transactions + tx,
            notifications = st.notifications + notif,
            apps = updatedApps,
            successMessage = "👑 PRO Developer Plan Activated! Ad-Free for 30 days, +500 Coins credited. Your app is pinned on Home Screen (+50 Coins per install)."
        )
    }

    LocalPersistence.saveApps(updatedApps)

    FirestoreRepository.syncToFirestore {
        FirestoreRepository.saveUser(updatedUser)
        FirestoreRepository.saveTransaction(tx)
        FirestoreRepository.saveNotification(notif)
        updatedApps.filter { it.developerId == current.userId }.forEach {
            FirestoreRepository.saveApp(it)
        }
    }
    return true
}

fun TestPilotRepository.rechargeVip1400Plan(paymentReference: String, serverVerified: Boolean = false): Boolean {
    // Real-money recharge requires server-side payment verification.
    // Client-supplied payment references without server verification are rejected.
    if (!serverVerified) {
        _state.update { it.copy(errorMessage = "Payment must be verified by server before premium activation.") }
        return false
    }
    val current = _state.value.currentUser ?: return false
    val now = System.currentTimeMillis()
    val validityDuration = 90L * 24L * 60L * 60L * 1000L // 90 days (3 Months)
    val currentExpiry = if (current.isPremiumActive) current.premiumUntilEpochMs else now
    val newExpiry = currentExpiry + validityDuration
    val bonusCoins = 1500
    val newBalance = current.coinBalance + bonusCoins

    val tx = CoinTransaction(
        transactionId = "vip_1400_${current.userId}_${now}",
        userId = current.userId,
        type = TransactionType.REAL_MONEY_RECHARGE,
        amount = bonusCoins,
        balanceAfter = newBalance,
        referenceId = paymentReference,
        description = "₹1400 3-Month VIP Plan Recharge (90-Day Ad-Free, #1 Top Pinned Spotlight, +1500 Coins, 100% Guaranteed Google Play Store Production Access Grant)",
        verificationMethod = "SERVER_ATOMIC_LEDGER",
        createdAtEpochMs = now
    )

    val updatedUser = current.copy(
        isPremium = true,
        premiumUntilEpochMs = newExpiry,
        premiumTier = "VIP_3_MONTH",
        coinBalance = newBalance,
        updatedAtEpochMs = now
    )

    val notif = AppNotification(
        notificationId = "notif_vip_1400_${current.userId}_${now}",
        userId = current.userId,
        title = "👑 3-Month VIP Activated! (90 Days) 🛡️",
        body = "Thank you for recharging ₹1400! 100% Guaranteed Google Play Store Production Access Protection Grant active. Your apps are pinned at the #1 Top Spot on Home Screen, testers earn 70 coins, and you have 90 days of ad-free testing with +1500 bonus coins!",
        type = NotificationType.REWARD,
        deepLinkRoute = "coin_wallet",
        createdAtEpochMs = now
    )

    // Upgrade existing developer apps to VIP #1 Top Pinned Spotlight status (+70 Coins)
    // We DO NOT create dummy apps anymore. If they have none, they just get the coins and premium status.
    val updatedApps = _state.value.apps.map { app ->
        if (app.developerId == current.userId) {
            app.copy(
                moderationStatus = ModerationStatus.APPROVED,
                isFeaturedPremium = true,
                isVipTopPinned = true,
                rewardPerTesterCoins = 70,
                updatedAtEpochMs = now
            )
        } else app
    }

    _state.update { st ->
        st.copy(
            currentUser = updatedUser,
            users = st.users.map { if (it.userId == current.userId) updatedUser else it },
            transactions = st.transactions + tx,
            notifications = st.notifications + notif,
            apps = updatedApps,
            successMessage = "👑 3-Month VIP Activated! 90 Days Ad-Free, #1 Top Pinned Spotlight (+70 Coins per install), +1500 Coins & Play Store Production Guarantee Active."
        )
    }

    LocalPersistence.saveApps(updatedApps)

    FirestoreRepository.syncToFirestore {
        FirestoreRepository.saveUser(updatedUser)
        FirestoreRepository.saveTransaction(tx)
        FirestoreRepository.saveNotification(notif)
        updatedApps.filter { it.developerId == current.userId }.forEach {
            FirestoreRepository.saveApp(it)
        }
    }
    return true
}
