package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

data class FeedbackRequest(
    val assignmentId: String,
    val category: FeedbackCategory,
    val severity: FeedbackSeverity,
    val summary: String,
    val reproductionSteps: String,
    val deviceInfo: String,
    val screenshotUrl: String? = null
)

fun TestPilotRepository.submitTesterFeedback(request: FeedbackRequest): Boolean {
    val assignment = _state.value.assignments.firstOrNull { it.assignmentId == request.assignmentId } ?: return false
    if (request.summary.trim().length !in 10..400) {
        _state.update { it.copy(errorMessage = "Feedback summary must be between 10 and 400 characters.") }
        return false
    }
    val fbId = "fb_${request.assignmentId}_${System.currentTimeMillis()}"
    val feedback = TesterFeedback(
        feedbackId = fbId,
        assignmentId = assignment.assignmentId,
        appId = assignment.appId,
        appName = assignment.appName,
        developerId = assignment.developerId,
        testerId = assignment.testerId,
        testerDisplayName = assignment.testerDisplayName,
        category = request.category,
        severity = request.severity,
        summary = request.summary.trim(),
        reproductionSteps = request.reproductionSteps.trim(),
        deviceInfo = request.deviceInfo.trim(),
        screenshotUrl = request.screenshotUrl?.trim()
    )
    _state.update { st ->
        st.copy(
            feedback = st.feedback + feedback,
            assignments = st.assignments.map { if (it.assignmentId == assignment.assignmentId) it.copy(feedbackSubmitted = true) else it }
        )
    }
    addNotification(assignment.developerId, "New Tester Feedback", "${assignment.testerDisplayName} submitted ${request.category.label.lowercase()} for ${assignment.appName}.", NotificationType.FEEDBACK, "my_apps")
    // Attempt completion reward now that feedback artifact exists (if all requirements met).
    creditCompletionReward(assignment.assignmentId)
    _state.update { it.copy(successMessage = "Feedback submitted. Thank you for honest testing!", isLoading = false) }
    return true
}

fun TestPilotRepository.markFeedbackHelpful(feedbackId: String): Boolean {
    val current = _state.value.currentUser ?: return false
    val fb = _state.value.feedback.firstOrNull { it.feedbackId == feedbackId && it.developerId == current.userId } ?: return false
    if (fb.isMarkedHelpfulByDeveloper) return false
    _state.update { st ->
        st.copy(feedback = st.feedback.map { if (it.feedbackId == feedbackId) it.copy(isMarkedHelpfulByDeveloper = true) else it })
    }
    val bonus = _state.value.rewardConfig.helpfulFeedbackBonusCoins
    val credited = creditUserInternal(
        userId = fb.testerId,
        type = TransactionType.HELPFUL_FEEDBACK_BONUS,
        amount = bonus,
        transactionId = "feedback_bonus_${feedbackId}",
        referenceId = feedbackId,
        description = "Developer marked your feedback on ${fb.appName} as helpful"
    )
    if (credited) {
        _state.update { st -> st.copy(feedback = st.feedback.map { if (it.feedbackId == feedbackId) it.copy(bonusRewardCredited = true) else it }) }
        addNotification(fb.testerId, "Helpful Feedback Bonus (+$bonus Coins)", "Your feedback on ${fb.appName} was marked helpful by the developer.", NotificationType.REWARD, "coin_wallet")
    }
    return credited
}
