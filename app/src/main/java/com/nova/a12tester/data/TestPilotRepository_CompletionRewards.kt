package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

fun TestPilotRepository.creditCompletionReward(assignmentId: String): Boolean {
    val assignment = _state.value.assignments.firstOrNull { it.assignmentId == assignmentId } ?: return false
    if (assignment.rewardCredited) return false
    val requires = assignment.reportedDaysCount >= assignment.requiredDays &&
        assignment.groupJoinedConfirmed && assignment.optInConfirmed && assignment.installConfirmed
    if (!requires) {
        _state.update { it.copy(errorMessage = "Completion requires group join, opt-in, install, and ${assignment.requiredDays} daily check-ins (all recorded through TestPilot).") }
        return false
    }
    val reward = assignment.rewardCoins
    val credited = creditUserInternal(
        userId = assignment.testerId,
        type = TransactionType.TESTING_COMPLETION_REWARD,
        amount = reward,
        transactionId = "completion_reward_${assignmentId}",
        referenceId = assignmentId,
        description = "Completed closed testing for ${assignment.appName}"
    ) { user -> user.copy(completedTestsCount = user.completedTestsCount + 1) }
    if (credited) {
        _state.update { st ->
            val updatedAssignments = st.assignments.map {
                if (it.assignmentId == assignmentId) it.copy(rewardCredited = true, status = AssignmentStatus.COMPLETED, completedAtEpochMs = System.currentTimeMillis()) else it
            }
            val updatedGroups = st.groups.map { g ->
                if (g.groupId == assignment.groupId) {
                    val allDone = g.participantIds.all { id ->
                        val a = updatedAssignments.firstOrNull { it.groupId == assignment.groupId && it.testerId == id }
                        a == null || a.status == AssignmentStatus.COMPLETED
                    }
                    if (allDone) g.copy(status = GroupStatus.COMPLETED) else g
                } else g
            }
            st.copy(assignments = updatedAssignments, groups = updatedGroups)
        }
        addNotification(assignment.testerId, "Testing Reward (+$reward Coins)", "You completed the closed test for ${assignment.appName}.", NotificationType.REWARD, "coin_wallet")
    }
    return credited
}

/**
 * AdMob rewarded-ad completion. Coins are granted ONLY after a genuine rewarded-ad completion
 * callback, and the [adTransactionNonce] must be unique (prevents replay of a single ad result).
 * Never rewards for merely clicking or loading an ad; complies with AdMob policy.
 */
fun TestPilotRepository.creditRewardedAdCompletion(adTransactionNonce: String): Boolean {
    val current = _state.value.currentUser ?: return false
    val reward = _state.value.rewardConfig.rewardedAdCoins
    val credited = creditUserInternal(
        userId = current.userId,
        type = TransactionType.REWARDED_AD,
        amount = reward,
        transactionId = "ad_reward_${adTransactionNonce}",
        referenceId = adTransactionNonce,
        description = "Rewarded ad completed",
        verificationMethod = "ADMOB_SSV_CALLBACK"
    )
    _state.update { st ->
        st.copy(
            successMessage = if (credited) "Reward earned: +$reward coins" else st.successMessage,
            errorMessage = if (credited) null else "This ad reward was already claimed.",
            isLoading = false
        )
    }
    return credited
}

fun TestPilotRepository.adminAdjustCoins(userId: String, amount: Int, note: String): Boolean {
    val admin = _state.value.currentUser ?: return false
    if (!admin.isAdminClaimVerified) return false
    val op = if (amount >= 0) "credit" else "debit"
    val idempotencyKey = "admin_${userId}_${System.currentTimeMillis()}"
    val ok = if (amount >= 0) {
        creditUserInternal(userId, TransactionType.ADMIN_ADJUSTMENT, amount, idempotencyKey, admin.userId, "Admin adjustment: $note", "ADMIN_MANUAL")
    } else {
        debitUserInternal(userId, TransactionType.ADMIN_ADJUSTMENT, -amount, idempotencyKey, admin.userId, "Admin adjustment: $note")
    }
    if (ok) addAuditLog(admin, "ADJUST_COINS", userId, "$op ${kotlin.math.abs(amount)} coins. $note")
    return ok
}
