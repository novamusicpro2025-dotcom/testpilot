package com.nova.a12tester.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.a12tester.domain.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Thin UI-facing facade over [TestPilotRepository]. In LIVE_FIREBASE mode each of these calls
 * would instead invoke a Cloud Function and observe Firestore; the repository keeps the same
 * security invariants (server-authoritative ledger, idempotency keys, verified admin claims).
 */
class TestPilotViewModel : ViewModel() {

    fun signInGoogle(email: String, name: String, photo: String) {
        TestPilotRepository.completeGoogleSignIn(email, name, photo, "uid_live_${UUID.randomUUID()}")
    }

    fun chooseRole(role: UserRole) = TestPilotRepository.acceptPrivacyConsentAndChooseRole(role, 34)
    fun setRole(role: UserRole) = TestPilotRepository.switchRole(role)
    fun setTheme(mode: ThemeMode) = TestPilotRepository.setThemeMode(mode)
    fun updateName(name: String) = TestPilotRepository.updateDisplayName(name)
    fun signOut() = TestPilotRepository.signOut()
    fun deleteAccount() = viewModelScope.launch { TestPilotRepository.deleteCurrentUserAccountAndData() }

    fun submitApp(request: SubmitAppRequest): Boolean = TestPilotRepository.submitAppForReview(request)
    fun cancelSubmission(appId: String) = TestPilotRepository.cancelAppSubmission(appId)

    fun joinGroup(appId: String) = viewModelScope.launch { TestPilotRepository.requestAutomaticGroupAllocation(appId) }
    fun confirmGroupJoined(id: String) = viewModelScope.launch { TestPilotRepository.confirmGroupJoined(id) }
    fun confirmOptedIn(id: String) = viewModelScope.launch { TestPilotRepository.confirmOptedIn(id) }
    fun confirmInstalled(id: String) = viewModelScope.launch { TestPilotRepository.confirmInstalled(id) }
    fun installAndRewardApp(appId: String): Int = TestPilotRepository.installAndRewardApp(appId)
    fun dailyCheckIn(id: String, note: String, screenshotUrl: String? = null): Boolean = TestPilotRepository.recordDailyCheckIn(id, note, screenshotUrl)
    fun reportInstallIssue(id: String, details: String) = viewModelScope.launch { TestPilotRepository.reportInstallationProblem(id, details) }
    fun claimCompletionReward(id: String) = viewModelScope.launch { TestPilotRepository.creditCompletionReward(id) }
    fun submitFeedback(request: FeedbackRequest): Boolean = TestPilotRepository.submitTesterFeedback(request)

    fun sendMessage(appId: String, assignmentId: String, receiverId: String, text: String): Boolean =
        TestPilotRepository.sendMessage(appId, assignmentId, receiverId, text)

    fun enforceDropOffs() {
        // Will be called periodically or by admin/trigger
        TestPilotRepository.enforceTesterDropOff()
    }

    fun watchRewardedAd() = viewModelScope.launch { TestPilotRepository.creditRewardedAdCompletion("ad_${UUID.randomUUID()}") }
    fun markNotifRead(id: String) = TestPilotRepository.markNotificationRead(id)
    fun markAllRead() = TestPilotRepository.markAllNotificationsRead()

    fun reportApp(appId: String, reason: ReportReason, details: String): Boolean = TestPilotRepository.reportApp(appId, reason, details)

    fun approve(appId: String) = viewModelScope.launch { TestPilotRepository.approveApp(appId) }
    fun reject(appId: String, reason: String) = viewModelScope.launch { TestPilotRepository.rejectApp(appId, reason) }
    fun suspendUser(userId: String, reason: String) = viewModelScope.launch { TestPilotRepository.suspendUser(userId, reason) }
    fun restoreUser(userId: String) = viewModelScope.launch { TestPilotRepository.restoreUser(userId) }
    fun resolveReport(id: String, remove: Boolean, notes: String) = viewModelScope.launch { TestPilotRepository.resolveReport(id, remove, notes) }
    fun replaceTester(assignmentId: String) = viewModelScope.launch { TestPilotRepository.replaceInactiveTester(assignmentId) }

    // Aliases matching screen call sites + admin tools.
    val state = TestPilotRepository.state
    fun consumeMessages() = TestPilotRepository.consumeMessages()
    fun markHelpful(id: String) = viewModelScope.launch { TestPilotRepository.markFeedbackHelpful(id) }
    fun adjustCoins(userId: String, amount: Int, note: String) = viewModelScope.launch { TestPilotRepository.adminAdjustCoins(userId, amount, note) }
    fun creditAdReward(nonce: String) = viewModelScope.launch { TestPilotRepository.creditRewardedAdCompletion(nonce) }
    fun readNotif(id: String) = TestPilotRepository.markNotificationRead(id)
    fun readAll() = TestPilotRepository.markAllNotificationsRead()
    fun report(appId: String, reason: ReportReason, details: String) = TestPilotRepository.reportApp(appId, reason, details)
    fun approveApp(id: String) = viewModelScope.launch { TestPilotRepository.approveApp(id) }
    fun rejectApp(id: String, reason: String) = viewModelScope.launch { TestPilotRepository.rejectApp(id, reason) }
    fun signIn(email: String, name: String, photo: String, uid: String) = TestPilotRepository.completeGoogleSignIn(email, name, photo, uid)
    fun completeRewardedAd(nonce: String): Boolean = TestPilotRepository.creditRewardedAdCompletion(nonce)

    fun recordUsage(assignmentId: String, additionalSeconds: Int): Boolean =
        TestPilotRepository.recordAppUsage(assignmentId, additionalSeconds)

    fun getDailyQuota(testerId: String): DailyTesterQuotaStatus =
        TestPilotRepository.getDailyTesterQuotaStatus(testerId)

    fun enforceDailyQuotaAudit(): Int =
        TestPilotRepository.enforceDailyTestingQuotaAudit()

    fun reinstateApp(appId: String): Boolean =
        TestPilotRepository.reinstateAppIfDailyQuotaMet(appId)

    fun joinTeam(appId: String, teamId: String? = null): Boolean =
        TestPilotRepository.joinCohortTeam(appId, teamId)


    /** Play Store recharge: ₹500 → 1-Month PRO (+500 Coins, ad-free, app pinned). */
    fun rechargeRealMoneyPro(orderId: String): Boolean =
        TestPilotRepository.rechargePremium500Plan(orderId, serverVerified = true)

    /** Play Store recharge: ₹1400 → 3-Month VIP (+1500 Coins, ad-free, #1 top spotlight). */
    fun rechargeRealMoneyVip(orderId: String): Boolean =
        TestPilotRepository.rechargeVip1400Plan(orderId, serverVerified = true)
}
