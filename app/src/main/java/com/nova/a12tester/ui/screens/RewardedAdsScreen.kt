package com.nova.a12tester.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.AdMobManager
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.ui.components.ComplianceNote
import java.util.UUID

/**
 * Rewarded Ads Screen.
 * Hosts Google Mobile Ads SDK official Test Rewarded Ad.
 * Calls [TestPilotViewModel.completeRewardedAd] when user completes watching the ad,
 * rewarding +15 coins in real-time.
 */
@Composable
fun RewardedAdsScreen(vm: TestPilotViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by TestPilotRepository.state.collectAsState()
    val reward = state.rewardConfig.rewardedAdCoins
    var watchedCount by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val isAdReady by AdMobManager.isAdReady.collectAsState()
    val isAdLoading by AdMobManager.isLoading.collectAsState()
    val lastError by AdMobManager.lastError.collectAsState()

    // Ensure ad is loaded on entering this screen
    LaunchedEffect(Unit) {
        AdMobManager.loadRewardedAd(context)
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Filled.Slideshow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        Text("Earn Coins", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Watch a rewarded ad to earn $reward coins. Coins are credited instantly to your wallet upon full completion.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.MonetizationOn, null, tint = MaterialTheme.colorScheme.primary)
                Text("Reward per completed ad: $reward coins", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        if (watchedCount > 0) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Text("$watchedCount ad(s) completed (+${watchedCount * reward} coins earned!)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        statusMessage?.let { msg ->
            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        // Live Google AdMob Rewarded Ad trigger
        Button(
            onClick = {
                if (activity != null) {
                    if (isAdReady) {
                        statusMessage = null
                        AdMobManager.showRewardedAd(
                            activity = activity,
                            onUserEarnedReward = {
                                val nonce = UUID.randomUUID().toString()
                                if (vm.completeRewardedAd(nonce)) {
                                    watchedCount++
                                    statusMessage = "🎉 Successfully earned +$reward coins!"
                                }
                            },
                            onAdClosed = {
                                statusMessage = "Ad closed. Ready for next ad."
                            },
                            onAdFailed = { err ->
                                statusMessage = "Could not display ad: $err"
                            }
                        )
                    } else {
                        statusMessage = "Loading Google Test Ad... Please tap in a few seconds."
                        AdMobManager.loadRewardedAd(context)
                    }
                }
            },
            enabled = !isAdLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isAdLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("Loading Google Test Ad…")
            } else {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isAdReady) "Watch Rewarded Ad (+$reward Coins)" else "Load & Watch Ad (+$reward Coins)")
            }
        }

        if (lastError != null && !isAdReady && !isAdLoading) {
            OutlinedButton(
                onClick = { AdMobManager.loadRewardedAd(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry Loading Ad")
            }
        }

        TextButton(onClick = onDone) { Text("Back to wallet") }
        ComplianceNote("TestPilot complies with Google AdMob policies. Coins are credited only after genuine rewarded ad completion verified via callback.")
    }
}
