package com.nova.a12tester.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.nova.a12tester.data.AdMobManager
import com.nova.a12tester.data.SubmitAppRequest
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.ComplianceNote
import com.nova.a12tester.ui.theme.isAppInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitAppScreen(vm: TestPilotViewModel, onSubmitted: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by TestPilotRepository.state.collectAsState()
    val cost = state.rewardConfig.appSubmissionCostCoins // 300 coins
    val userBalance = state.currentUser?.coinBalance ?: 0
    val hasEnoughCoins = userBalance >= cost
    val isDark = isAppInDarkTheme()

    var appName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var selectedPresetIcon by remember { mutableStateOf("🚀") }
    var shortDesc by remember { mutableStateOf("") }
    var developerName by remember { mutableStateOf(state.currentUser?.displayName.orEmpty().ifBlank { "Android Dev" }) }
    var optInUrl by remember { mutableStateOf("") }
    var groupUrl by remember { mutableStateOf("") }
    var privacyUrl by remember { mutableStateOf("") }
    var minAndroid by remember { mutableIntStateOf(26) }
    var instructions by remember { mutableStateOf("Open and test daily for 2 minutes to keep reciprocal testing active.") }
    var durationDays by remember { mutableIntStateOf(14) }

    val presetIcons = listOf(
        "🚀" to "https://ui-avatars.com/api/?name=App&background=2563EB&color=fff",
        "⚡" to "https://ui-avatars.com/api/?name=Pro&background=EA580C&color=fff",
        "🎯" to "https://ui-avatars.com/api/?name=Goal&background=059669&color=fff",
        "📊" to "https://ui-avatars.com/api/?name=Stat&background=7C3AED&color=fff",
        "🎵" to "https://ui-avatars.com/api/?name=Beat&background=EC4899&color=fff",
        "🛡️" to "https://ui-avatars.com/api/?name=Safe&background=0284C7&color=fff",
        "🤖" to "https://ui-avatars.com/api/?name=AI&background=10B981&color=fff",
        "📷" to "https://ui-avatars.com/api/?name=Cam&background=F59E0B&color=fff"
    )

    val effectiveIconUrl = presetIcons.firstOrNull { it.first == selectedPresetIcon }?.second
        ?: "https://ui-avatars.com/api/?name=${appName.take(2).ifBlank { "AP" }}&background=2563EB&color=fff"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Submit Your App", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(
            "Every developer submits their app for 14-day closed testing. 300 coins are required to register your app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val myActiveApps = remember(state.apps) {
            state.apps.filter {
                it.developerId == state.currentUser?.userId &&
                    (it.moderationStatus == ModerationStatus.APPROVED || it.moderationStatus == ModerationStatus.PENDING_REVIEW)
            }
        }
        val isUserVip = state.currentUser?.isVipActive == true
        val isUserPremium = state.currentUser?.isPremiumActive == true
        val isOneMonthLimitReached = isUserPremium && !isUserVip && myActiveApps.isNotEmpty()

        if (isUserVip) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1435) else Color(0xFFEDE9FE),
                    contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E1B4B)
                ),
                border = BorderStroke(1.2.dp, if (isDark) Color(0xFFA78BFA) else Color(0xFF8B5CF6))
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("👑", style = MaterialTheme.typography.titleMedium)
                    Column(Modifier.weight(1f)) {
                        Text(
                            "3-Month VIP Active (UNLIMITED APPS) • 🛡️ Play Store Protected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = if (isDark) Color(0xFFEDE9FE) else Color(0xFF5B21B6)
                        )
                        Text(
                            "Submit as many apps as you want! All apps pinned at #1 TOP SPOT on Home Screen with 70 Coins tester reward & 100% Guaranteed Google Play Store Production Approval Protection active.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFDDD6FE) else Color(0xFF6D28D9)
                        )
                    }
                }
            }
        } else if (isOneMonthLimitReached) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            "1-App Quota Reached (1-Month PRO Plan)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        "Your 1-Month PRO Plan allows only 1 app for testing. You have already submitted '${myActiveApps.firstOrNull()?.appName}'. To submit UNLIMITED apps for testing, upgrade to the 3-Month VIP Plan (₹1400) in your Wallet!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else if (isUserPremium) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1F1A12) else Color(0xFFFEF3C7),
                    contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                ),
                border = BorderStroke(1.dp, Color(0xFFF59E0B))
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⭐", style = MaterialTheme.typography.titleMedium)
                    Column(Modifier.weight(1f)) {
                        Text(
                            "1-Month PRO Active (1 App Included) • 🛡️ Play Store Protected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF92400E)
                        )
                        Text(
                            "1 App included for testing with 100% Guaranteed Google Play Store Production Approval Protection! Pinned as PRO Featured with 50 Coins tester reward.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                        )
                    }
                }
            }
        }

        // Coin requirement & status card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (hasEnoughCoins) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.MonetizationOn,
                        contentDescription = null,
                        tint = if (hasEnoughCoins) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Submission Cost: $cost coins  •  Your Balance: $userBalance coins",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = if (hasEnoughCoins) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                if (!hasEnoughCoins) {
                    val needed = cost - userBalance
                    Text(
                        "You need $needed more coins to submit your app. Watch rewarded ads to earn 15 coins each!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = {
                            if (activity != null) {
                                AdMobManager.showRewardedAd(
                                    activity = activity,
                                    onUserEarnedReward = {
                                        vm.completeRewardedAd(java.util.UUID.randomUUID().toString())
                                    },
                                    onAdFailed = {
                                        AdMobManager.loadRewardedAd(context)
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Slideshow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Watch Rewarded Ad (+15 Coins)")
                    }
                }
            }
        }

        // App Icon section with Preview
        Text("App Icon", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(selectedPresetIcon, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Selected Icon Preview", style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Text("Pick an icon preset below or enter a custom image URL.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presetIcons.forEach { (emoji, _) ->
                FilterChip(
                    selected = selectedPresetIcon == emoji,
                    onClick = {
                        selectedPresetIcon = emoji
                    },
                    label = { Text(emoji, style = MaterialTheme.typography.titleMedium) }
                )
            }
        }

        FormField("App Name", appName, { appName = it }, "e.g., FocusFlow Pomodoro")
        FormField("Package Name", packageName, { packageName = it }, "com.yourcompany.appname")
        FormField("Developer / Studio Name", developerName, { developerName = it }, "Your studio name")
        FormField("Short Description", shortDesc, { shortDesc = it }, "15-300 characters describing your app", minLines = 2)
        FormField("Google Play Closed-Testing Opt-In URL", optInUrl, { optInUrl = it }, "https://play.google.com/apps/testing/<package>")
        FormField("Google Groups Membership URL", groupUrl, { groupUrl = it }, "https://groups.google.com/g/<your-testers>")
        FormField("Privacy Policy URL (if applicable)", privacyUrl, { privacyUrl = it }, "https://yourdomain.com/privacy", required = false)
        FormField("Testing Instructions", instructions, { instructions = it }, "Steps for testers (Daily 2 minutes)", minLines = 2)

        Text("Required Android Version: API $minAndroid", style = MaterialTheme.typography.titleSmall)
        Slider(value = minAndroid.toFloat(), onValueChange = { minAndroid = it.toInt() }, valueRange = 21f..35f, steps = 13)

        Text("Testing Duration: $durationDays days", style = MaterialTheme.typography.titleSmall)
        Slider(value = durationDays.toFloat(), onValueChange = { durationDays = it.toInt() }, valueRange = 14f..30f, steps = 15)

        ComplianceNote("You must not request fake reviews or ask for Google passwords. Developers remain responsible for meeting Google Play's 14-day closed testing rules.")

        Spacer(Modifier.height(8.dp))
        Button(
            enabled = hasEnoughCoins && !isOneMonthLimitReached,
            onClick = {
                val cleanGroup = ValidationEngine.cleanGoogleGroupUrl(groupUrl)
                val ok = vm.submitApp(
                    SubmitAppRequest(
                        appName = appName.trim(),
                        packageName = packageName.trim(),
                        iconUrl = effectiveIconUrl.trim(),
                        shortDescription = shortDesc.trim(),
                        developerName = developerName.trim(),
                        optInUrl = optInUrl.trim(),
                        googleGroupUrl = cleanGroup,
                        privacyPolicyUrl = privacyUrl.trim(),
                        minAndroidVersion = minAndroid,
                        testingInstructions = instructions.trim(),
                        testingDurationDays = durationDays
                    )
                )
                if (ok) onSubmitted()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                when {
                    isOneMonthLimitReached -> "1-App Limit Reached (Upgrade to 3-Month VIP)"
                    hasEnoughCoins -> "Submit App ($cost Coins)"
                    else -> "$cost Coins Required to Submit"
                }
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun FormField(label: String, value: String, onChange: (String) -> Unit, placeholder: String, required: Boolean = true, minLines: Int = 1) {
    Column {
        OutlinedTextField(
            value = value, onValueChange = onChange,
            label = { Text(if (required) label else "$label (optional)") },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(), minLines = minLines,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Text)
        )
    }
}
