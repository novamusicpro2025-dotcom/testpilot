package com.nova.a12tester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.nova.a12tester.data.*
import com.nova.a12tester.ui.TestPilotApp
import com.nova.a12tester.ui.theme.TestPilotTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TestPilotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Google Mobile Ads SDK and preload test rewarded ads
        AdMobManager.initialize(this)
        
        // Initialize Billing Manager
        BillingManager.initialize(this)

        // Initialize offline local persistence immediately
        LocalPersistence.init(this)

        // 1. Immediately restore state from local storage (preserves coins, profile, teams, transactions offline & instantly)
        val localUser = LocalPersistence.loadUser()
        val now = System.currentTimeMillis()

        if (localUser != null && (now - localUser.lastActiveAtEpochMs > com.nova.a12tester.domain.INACTIVITY_PURGE_MILLIS)) {
            // User was inactive for >= 90 days (3 months): Erase user data from server and reset local state
            val uidToPurge = localUser.userId
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    FirestoreRepository.deleteUserData(uidToPurge)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to delete user data from Firestore: ${e.message}")
                }
                LocalPersistence.clearAll()
                FirebaseAuthManager.signOut(applicationContext)
            }
            TestPilotRepository.signOut()
        } else if (localUser != null) {
            // User is active: refresh activity timestamps and extend purge window by 3 months (90 days)
            val refreshedUser = localUser.copy(
                lastActiveAtEpochMs = now,
                purgeAfterEpochMs = now + com.nova.a12tester.domain.INACTIVITY_PURGE_MILLIS,
                updatedAtEpochMs = now
            )
            LocalPersistence.saveUser(refreshedUser)
            FirestoreRepository.syncToFirestore {
                FirestoreRepository.saveUser(refreshedUser)
            }

            TestPilotRepository.restoreUserSession(refreshedUser)
            val localTxs = LocalPersistence.loadTransactions()
            val localApps = LocalPersistence.loadApps()
            val localTeams = LocalPersistence.loadCohortTeams()
            val localSessions = LocalPersistence.loadUsageSessions()
            val localAssignments = LocalPersistence.loadAssignments()
            TestPilotRepository.mergeUserData(
                assignments = localAssignments,
                transactions = localTxs,
                notifications = emptyList(),
                usageSessions = localSessions,
                cohortTeams = localTeams,
                apps = localApps
            )
            TestPilotRepository.setOnboardingComplete(refreshedUser.privacyConsentAccepted)
        } else {
            // Fallback for previous legacy prefs if available
            val prefs = getSharedPreferences("testpilot_prefs", android.content.Context.MODE_PRIVATE)
            val savedUserId = prefs.getString("saved_user_id", null)
            if (savedUserId != null) {
                val savedEmail = prefs.getString("saved_user_email", "").orEmpty()
                val savedName = prefs.getString("saved_user_name", "").orEmpty()
                TestPilotRepository.completeGoogleSignIn(
                    googleEmail = savedEmail,
                    googleDisplayName = savedName,
                    googlePhotoUrl = "",
                    firebaseUid = savedUserId
                )
                TestPilotRepository.acceptPrivacyConsentAndChooseRole(com.nova.a12tester.domain.UserRole.BOTH, 35)
            }
        }

        // 2. In background, if Firebase Auth or network is available, sync latest from Firestore
        val firebaseUser = FirebaseAuthManager.currentFirebaseUser
        val uidToSync = firebaseUser?.uid ?: localUser?.userId
        if (uidToSync != null) {
            lifecycleScope.launch {
                try {
                    val storedUser = kotlinx.coroutines.withTimeoutOrNull(3000L) {
                        FirestoreRepository.loadUser(uidToSync)
                    }
                    if (storedUser != null) {
                        val hasAdminClaim = FirebaseAuthManager.hasAdminClaim()
                        TestPilotRepository.restoreUserSession(
                            storedUser.copy(isAdminClaimVerified = hasAdminClaim)
                        )

                        kotlinx.coroutines.withTimeoutOrNull(5000L) {
                            val assignments = FirestoreRepository.loadAssignmentsForUser(uidToSync)
                            val transactions = FirestoreRepository.loadTransactionsForUser(uidToSync)
                            val notifications = FirestoreRepository.loadNotificationsForUser(uidToSync)
                            val sessions = FirestoreRepository.loadUsageSessionsForUser(uidToSync)
                            val teams = FirestoreRepository.loadAllTeams()
                            val apps = FirestoreRepository.loadAllApps()

                            TestPilotRepository.mergeUserData(
                                assignments = assignments,
                                transactions = transactions,
                                notifications = notifications,
                                usageSessions = sessions,
                                cohortTeams = teams,
                                apps = apps
                            )

                            // Perform daily quota enforcement audit for yesterday
                            TestPilotRepository.enforceDailyTestingQuotaAudit()
                            
                            // Perform drop-off prevention & auto-replacements
                            TestPilotRepository.enforceTesterDropOff()
                            
                            com.nova.a12tester.data.UsageStatsHelper.syncRealDeviceUsage(applicationContext)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "Firestore sync failed — user remains on cached local state: ${e.message}")
                }
            }
        }

        setContent {
            val state by TestPilotRepository.state.collectAsState()
            TestPilotTheme(themeMode = state.themeMode) {
                TestPilotApp(vm = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.nova.a12tester.data.UsageStatsHelper.syncRealDeviceUsage(this)
    }

    /**
     * A native method that is implemented by the 'a12tester' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'a12tester' library on application startup.
        init {
            try {
                System.loadLibrary("a12tester")
            } catch (e: UnsatisfiedLinkError) {
                // Ignore if not present in architecture
            }
        }
    }
}