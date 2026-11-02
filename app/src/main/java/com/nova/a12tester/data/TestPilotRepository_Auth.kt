package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

/**
 * Authentication + account lifecycle. Google Sign-In never requests Google passwords; in
 * LIVE_FIREBASE mode the credential is exchanged through the standard Google Sign-In flow and
 * then minted with FirebaseAuth.
 */
fun TestPilotRepository.beginGoogleSignIn() {
    _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
}

fun TestPilotRepository.completeGoogleSignIn(
    googleEmail: String,
    googleDisplayName: String,
    googlePhotoUrl: String,
    firebaseUid: String
) {
    var resolvedUser: UserProfile? = null
    _state.update { st ->
        val existing = st.users.firstOrNull { it.email.equals(googleEmail, ignoreCase = true) }
        val resolved = if (existing != null) {
            existing.copy(
                userId = firebaseUid,
                displayName = googleDisplayName.ifBlank { existing.displayName },
                photoUrl = googlePhotoUrl.ifBlank { existing.photoUrl },
                updatedAtEpochMs = System.currentTimeMillis()
            )
        } else {
            UserProfile(
                userId = firebaseUid,
                displayName = googleDisplayName.ifBlank { googleEmail.substringBefore('@') },
                email = googleEmail,
                photoUrl = googlePhotoUrl,
                role = UserRole.BOTH,
                coinBalance = 0,
                pendingRewardCoins = 0,
                reputationScore = 100,
                completedTestsCount = 0,
                submittedAppsCount = 0,
                androidSdkVersion = 34,
                accountStatus = AccountStatus.ACTIVE,
                isAdminClaimVerified = false,
                privacyConsentAccepted = false,
                createdAtEpochMs = System.currentTimeMillis()
            )
        }
        resolvedUser = resolved
        val users = if (existing != null) st.users.map { if (it.userId == existing.userId) resolved else it } else st.users + resolved
        st.copy(
            users = users,
            currentUser = resolved,
            isLoading = false,
            successMessage = "Signed in as ${resolved.displayName}",
            hasCompletedOnboarding = existing?.privacyConsentAccepted == true
        )
    }
    resolvedUser?.let { u ->
        LocalPersistence.saveUser(u)
        FirestoreRepository.syncToFirestore {
            FirestoreRepository.saveUser(u)
        }
    }
}

fun TestPilotRepository.acceptPrivacyConsentAndChooseRole(role: UserRole, deviceSdk: Int) {
    val current = _state.value.currentUser ?: return
    val updated = current.copy(
        role = role,
        privacyConsentAccepted = true,
        androidSdkVersion = deviceSdk,
        updatedAtEpochMs = System.currentTimeMillis()
    )
    val config = _state.value.rewardConfig
    val welcomeBonus = if (updated.coinBalance == 0) config.welcomeBonusCoins else 0
    val finalUser = updated.copy(coinBalance = updated.coinBalance + welcomeBonus)

    val welcomeTx = if (welcomeBonus > 0) {
        CoinTransaction(
            transactionId = "welcome_${updated.userId}",
            userId = updated.userId,
            type = TransactionType.WELCOME_BONUS,
            amount = welcomeBonus,
            balanceAfter = finalUser.coinBalance,
            referenceId = "onboarding",
            description = "New community member welcome bonus",
            verificationMethod = "SERVER_ATOMIC_LEDGER"
        )
    } else null

    val welcomeNotif = if (welcomeBonus > 0) {
        AppNotification(
            notificationId = "notif_welcome_${updated.userId}",
            userId = updated.userId,
            title = "Welcome Bonus Credited",
            body = "You received $welcomeBonus coins for joining the TestPilot community.",
            type = NotificationType.REWARD,
            deepLinkRoute = "coin_wallet"
        )
    } else null

    _state.update { st ->
        st.copy(
            users = st.users.map { if (it.userId == finalUser.userId) finalUser else it },
            currentUser = finalUser,
            transactions = if (welcomeTx != null) st.transactions + welcomeTx else st.transactions,
            notifications = if (welcomeNotif != null) st.notifications + welcomeNotif else st.notifications,
            hasCompletedOnboarding = true
        )
    }
    LocalPersistence.saveUser(finalUser)
    FirestoreRepository.syncToFirestore {
        FirestoreRepository.saveUser(finalUser)
        if (welcomeTx != null) FirestoreRepository.saveTransaction(welcomeTx)
    }
}

fun TestPilotRepository.updateDisplayName(newName: String) {
    val current = _state.value.currentUser ?: return
    val trimmed = newName.trim()
    if (trimmed.length !in 2..60) {
        _state.update { it.copy(errorMessage = "Display name must be between 2 and 60 characters.") }
        return
    }
    val updated = current.copy(displayName = trimmed, updatedAtEpochMs = System.currentTimeMillis())
    _state.update { st ->
        st.copy(
            users = st.users.map { if (it.userId == updated.userId) updated else it },
            currentUser = updated,
            successMessage = "Profile updated"
        )
    }
}

fun TestPilotRepository.switchRole(role: UserRole) {
    val current = _state.value.currentUser ?: return
    val updated = current.copy(role = role, updatedAtEpochMs = System.currentTimeMillis())
    _state.update { st ->
        st.copy(
            users = st.users.map { if (it.userId == updated.userId) updated else it },
            currentUser = updated
        )
    }
}

fun TestPilotRepository.signOut() {
    FirebaseAuthManager.signOut()
    _state.update { st ->
        st.copy(
            currentUser = null,
            isLoading = false,
            hasCompletedOnboarding = false,
            errorMessage = null,
            successMessage = null
        )
    }
}
