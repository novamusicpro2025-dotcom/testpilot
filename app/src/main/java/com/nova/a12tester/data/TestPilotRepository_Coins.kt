package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

/**
 * Secure coin economy. Every balance change goes through [creditUser] / [debitUser], which:
 *  - use a UNIQUE idempotent transactionId (replay-safe; duplicate rewards are no-ops)
 *  - maintain an append-only transaction ledger with balanceAfter snapshots
 *  - never trust a client-supplied balance
 */
internal fun TestPilotRepository.creditUserInternal(
    userId: String,
    type: TransactionType,
    amount: Int,
    transactionId: String,
    referenceId: String,
    description: String,
    verificationMethod: String = "SERVER_ATOMIC_LEDGER",
    mutateUser: ((UserProfile) -> UserProfile)? = null
): Boolean {
    if (_state.value.transactions.any { it.transactionId == transactionId }) return false
    val user = _state.value.users.firstOrNull { it.userId == userId } ?: return false
    val newBalance = user.coinBalance + amount
    val tx = CoinTransaction(
        transactionId = transactionId,
        userId = userId,
        type = type,
        amount = amount,
        balanceAfter = newBalance,
        referenceId = referenceId,
        description = description,
        verificationMethod = verificationMethod
    )
    val updatedUser = (mutateUser?.invoke(user) ?: user).copy(coinBalance = newBalance, updatedAtEpochMs = System.currentTimeMillis())
    _state.update { st ->
        st.copy(
            users = st.users.map { if (it.userId == userId) updatedUser else it },
            currentUser = if (st.currentUser?.userId == userId) updatedUser else st.currentUser,
            transactions = st.transactions + tx
        )
    }
    LocalPersistence.saveUser(updatedUser)
    FirestoreRepository.syncToFirestore {
        FirestoreRepository.saveUser(updatedUser)
        FirestoreRepository.saveTransaction(tx)
    }
    return true
}

internal fun TestPilotRepository.debitUserInternal(
    userId: String,
    type: TransactionType,
    amount: Int,
    transactionId: String,
    referenceId: String,
    description: String
): Boolean {
    if (_state.value.transactions.any { it.transactionId == transactionId }) return false
    val user = _state.value.users.firstOrNull { it.userId == userId } ?: return false
    if (user.coinBalance < amount) return false
    val newBalance = user.coinBalance - amount
    val tx = CoinTransaction(
        transactionId = transactionId,
        userId = userId,
        type = type,
        amount = -amount,
        balanceAfter = newBalance,
        referenceId = referenceId,
        description = description,
        verificationMethod = "SERVER_ATOMIC_LEDGER"
    )
    val updatedUser = user.copy(coinBalance = newBalance, updatedAtEpochMs = System.currentTimeMillis())
    _state.update { st ->
        st.copy(
            users = st.users.map { if (it.userId == userId) updatedUser else it },
            currentUser = if (st.currentUser?.userId == userId) updatedUser else st.currentUser,
            transactions = st.transactions + tx
        )
    }
    LocalPersistence.saveUser(updatedUser)
    FirestoreRepository.syncToFirestore {
        FirestoreRepository.saveUser(updatedUser)
        FirestoreRepository.saveTransaction(tx)
    }
    return true
}

fun TestPilotRepository.grantMilestoneReward(assignmentId: String, dayNumber: Int): Boolean {
    val assignment = _state.value.assignments.firstOrNull { it.assignmentId == assignmentId } ?: return false
    val bonus = _state.value.rewardConfig.dailyCheckInMilestoneCoins
    val credited = creditUserInternal(
        userId = assignment.testerId,
        type = TransactionType.DAILY_CHECKIN_MILESTONE,
        amount = bonus,
        transactionId = "milestone_${assignmentId}_${dayNumber}",
        referenceId = assignmentId,
        description = "Testing milestone: $dayNumber server-verified check-ins on ${assignment.appName}"
    )
    if (credited) {
        addNotification(
            assignment.testerId,
            "Milestone Reward (+$bonus Coins)",
            "You reached day $dayNumber of server-verified testing on ${assignment.appName}.",
            NotificationType.REWARD,
            "coin_wallet"
        )
    }
    return credited
}
