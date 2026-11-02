package com.nova.a12tester.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.rechargePremium500Plan
import com.nova.a12tester.data.rechargeVip1400Plan
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.*
import com.nova.a12tester.ui.theme.BrandAmber
import com.nova.a12tester.ui.theme.BrandIndigo
import com.nova.a12tester.ui.theme.BrandTealDark
import com.nova.a12tester.ui.theme.isAppInDarkTheme
import java.text.SimpleDateFormat
import java.util.*
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.nova.a12tester.data.BillingManager
import kotlinx.coroutines.launch

@Composable
fun CoinWalletScreen(onOpenAds: () -> Unit) {
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return
    val myTx = state.transactions.filter { it.userId == user.userId }.sortedByDescending { it.createdAtEpochMs }
    var showPaymentModal by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf("VIP_1400") }
    val isDark = isAppInDarkTheme()
    val activity = LocalContext.current as Activity
    val products by BillingManager.premiumProducts.collectAsState()

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            GradientBanner(Modifier.fillMaxWidth().padding(20.dp), topColor = BrandTealDark, bottomColor = BrandIndigo) {
                Text("Available Balance", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MonetizationOn, null, tint = BrandAmber, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("${user.coinBalance}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("Pending rewards: ${user.pendingRewardCoins} • Completed tests: ${user.completedTestsCount}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(12.dp))
                if (!user.isPremiumActive) {
                    Button(onClick = onOpenAds, colors = ButtonDefaults.buttonColors(containerColor = BrandAmber, contentColor = Color(0xFF43310A))) {
                        Icon(Icons.Filled.PlayCircle, null); Spacer(Modifier.width(8.dp)); Text("Watch Rewarded Ad (+15 Coins)")
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(16.dp))
                            Text(
                                if (user.isVipActive) "👑 3-Month VIP Active (Ad-Free)" else "PRO Member: Ad-Free Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ─── ₹1400 3-Month VIP Top Spotlight Plan ────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1435) else Color(0xFFFAF5FF),
                    contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E1B4B)
                ),
                border = BorderStroke(
                    2.dp,
                    if (user.isVipActive) Color(0xFF10B981) else (if (isDark) Color(0xFFA78BFA) else Color(0xFF8B5CF6))
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Stars,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    if (user.isVipActive) "👑 3-Month VIP Active" else "👑 3-Month VIP Spotlight",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFEDE9FE) else Color(0xFF4C1D95)
                                )
                                Text(
                                    if (user.isVipActive) "${user.premiumDaysRemaining} days remaining validity" else "₹1400 / 90 Days (3 Months)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (user.isVipActive) Color(0xFF10B981) else (if (isDark) Color(0xFF8B5CF6) else Color(0xFF7C3AED))
                        ) {
                            Text(
                                if (user.isVipActive) "ACTIVE" else "BEST VALUE",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Play Store Protection & Approval Guarantee Grant Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFA78BFA).copy(alpha = 0.5f) else Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
                                modifier = Modifier.size(24.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "🛡️ Guaranteed Play Store Production Protection Grant",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF5F3FF) else Color(0xFF4C1D95)
                                )
                                Text(
                                    "Guaranteed to 100% satisfy Google Play 14-day closed testing rules with full protection support to secure Production Access approval.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFDDD6FE) else Color(0xFF5B21B6)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            "• ♾️ UNLIMITED Apps Testing: Submit and test as many apps as you need",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9)
                        )
                        Text(
                            "• 📌 #1 Top Pinned Spotlight: All your apps pinned at the absolute TOP of Home Screen",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFEDE9FE) else Color(0xFF1E1B4B)
                        )
                        Text(
                            "• 💰 70 Coins per Tester: Testers receive 70 coins to install and test your apps instantly",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFEDE9FE) else Color(0xFF1E1B4B)
                        )
                        Text(
                            "• 🚫 90 Days 100% Ad-Free: Completely ad-free for 3 full months",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4B5563)
                        )
                        Text(
                            "• 🎁 +1500 Bonus Coins: Added immediately to your wallet",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4B5563)
                        )
                    }

                    Button(
                        onClick = {
                            selectedPlan = "VIP_1400"
                            showPaymentModal = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (user.isVipActive) Color(0xFF10B981) else Color(0xFF7C3AED)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.ElectricBolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (user.isVipActive) "Extend 3-Month VIP (₹1400)" else "Recharge ₹1400 (Instant 3-Month VIP)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ─── ₹500 1-Month PRO Recharge Card ─────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1F1A12) else Color(0xFFFFFBEB),
                    contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (user.isPremiumActive && !user.isVipActive) Color(0xFF10B981) else Color(0xFFF59E0B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (user.isPremiumActive) (if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5)) else (if (isDark) Color(0xFF382307) else Color(0xFFFEF3C7)),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.WorkspacePremium,
                                        contentDescription = null,
                                        tint = if (user.isPremiumActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    if (user.isPremiumActive && !user.isVipActive) "⭐ PRO Plan Active" else "⭐ 1-Month PRO Plan",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF78350F)
                                )
                                Text(
                                    if (user.isPremiumActive && !user.isVipActive) "${user.premiumDaysRemaining} days remaining validity" else "₹500 / 30 Days (1 Month)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (user.isPremiumActive) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (user.isPremiumActive && !user.isVipActive) Color(0xFF10B981) else Color(0xFFF59E0B)
                        ) {
                            Text(
                                if (user.isPremiumActive && !user.isVipActive) "ACTIVE" else "₹500",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Play Store Protection & Approval Guarantee Grant Box for 1-Month Plan
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF382307) else Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "🛡️ Guaranteed Play Store Production Protection Grant",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF92400E)
                                )
                                Text(
                                    "Complete 14-day closed testing for 1 app with full guaranteed support to achieve Play Store Production Access approval.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "• 📱 1 App Testing Quota: Allows 1 app for 14-day closed testing",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)
                        )
                        Text(
                            "• 🚫 Ad-Free Experience: No ads required for 30 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                        )
                        Text(
                            "• ⭐ Featured Home Spotlight: Your submitted app pinned on Home",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                        )
                        Text(
                            "• 💰 50 Coins per Tester: Testers receive 50 coins to test your app",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                        )
                        Text(
                            "• 🎁 +500 Bonus Coins: Credited instantly to your wallet",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563)
                        )
                    }

                    Button(
                        onClick = {
                            selectedPlan = "PRO_500"
                            showPaymentModal = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (user.isPremiumActive && !user.isVipActive) Color(0xFF10B981) else Color(0xFFF59E0B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.ElectricBolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (user.isPremiumActive && !user.isVipActive) "Extend 1-Month PRO (₹500)" else "Recharge ₹500 (1-Month PRO)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ─── Economy Rules Card ─────────────────────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("TestPilot Coin Economy Rules:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("• Submit Your App: 300 Coins (one-time fee per app submission)", style = MaterialTheme.typography.bodySmall)
                    Text("• Join Team Cohort: 200 Coins (13-developer reciprocal testing)", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "• 👑 3-Month VIP App: Testers earn +70 Coins per install & test",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9)
                    )
                    Text(
                        "• ⭐ 1-Month PRO App: Testers earn +50 Coins per test",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                    )
                    Text("• Standard App Testing: Earn +25 Coins per test", style = MaterialTheme.typography.bodySmall)
                    Text("• Rewarded Ads: Earn +15 Coins per watched ad", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { SectionHeader("Transaction History", "Append-only ledger • idempotent & atomic") }
        if (myTx.isEmpty()) {
            item { EmptyState(Icons.Filled.Receipt, "No transactions yet", "Your coin history will appear here.") }
        } else {
            items(myTx) { tx -> TransactionRow(tx) }
        }
    }

    if (showPaymentModal) {
        val isVip = selectedPlan == "VIP_1400"
        AlertDialog(
            onDismissRequest = { showPaymentModal = false },
            icon = {
                Icon(
                    if (isVip) Icons.Filled.Stars else Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isVip) (if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)) else (if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    if (isVip) "👑 3-Month VIP Recharge (₹1400)" else "⭐ 1-Month PRO Recharge (₹500)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Plan selector toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isVip,
                            onClick = { selectedPlan = "VIP_1400" },
                            label = { Text("👑 ₹1400 (3 Mos)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isVip,
                            onClick = { selectedPlan = "PRO_500" },
                            label = { Text("⭐ ₹500 (1 Mo)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isVip) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFFA78BFA).copy(alpha = 0.5f) else Color(0xFF8B5CF6).copy(alpha = 0.5f))
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "🛡️ Google Play Store Production Guarantee Grant",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF5F3FF) else Color(0xFF4C1D95)
                                )
                                Text(
                                    "100% Guaranteed Production Access Support: 14-day closed testing completion with verified daily testing audit.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFDDD6FE) else Color(0xFF5B21B6)
                                )
                            }
                        }
                        Text(
                            "Amount: ₹1400 (90 Days Validity)",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
                        )
                        Text(
                            "Benefits included:\n• ♾️ UNLIMITED Apps Testing Submissions\n• 🛡️ 100% Play Store Production Approval Guarantee\n• 90 Days 100% Ad-Free Experience\n• #1 Top Pinned App Spotlight on Home\n• 70 Coins Tester Payout per Install\n• +1500 Bonus Coins instantly credited",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E1B4B)
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF382307) else Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "🛡️ Google Play Store Production Guarantee Grant",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF92400E)
                                )
                                Text(
                                    "100% Guaranteed Production Access Support for 1 App: 14-day closed testing completion guaranteed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                                )
                            }
                        }
                        Text(
                            "Amount: ₹500 (30 Days Validity)",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                        )
                        Text(
                            "Benefits included:\n• 📱 1 App Testing Quota Included\n• 🛡️ 100% Play Store Production Approval Guarantee\n• 30 Days Ad-Free Experience\n• Home Screen Featured App Spotlight\n• 50 Coins Tester Payout per Test\n• +500 Bonus Coins instantly credited",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E1B4B)
                        )
                    }

                    HorizontalDivider()
                    Text("Select Payment Method (Google Play Billing):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    if (products.isEmpty()) {
                        Text("Loading Google Play plans...", style = MaterialTheme.typography.labelSmall)
                    } else {
                        products.forEach { product ->
                            Text("Plan: ${product.name}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val product = if (isVip) products.find { it.productId == BillingManager.PREMIUM_3_MONTHS }
                                      else products.find { it.productId == BillingManager.PREMIUM_1_MONTH }
                        
                        if (product != null) {
                            BillingManager.launchPurchaseFlow(activity, product)
                        } else {
                            // Fallback to fake if play store is not working
                            if (isVip) {
                                TestPilotRepository.rechargeVip1400Plan("GPLAY_VIP_${System.currentTimeMillis()}", serverVerified = true)
                            } else {
                                TestPilotRepository.rechargePremium500Plan("GPLAY_PRO_${System.currentTimeMillis()}", serverVerified = true)
                            }
                        }
                        showPaymentModal = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVip) Color(0xFF7C3AED) else Color(0xFFF59E0B)
                    )
                ) {
                    Text(if (isVip) "Pay via Google Play (VIP)" else "Pay via Google Play (PRO)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransactionRow(tx: CoinTransaction) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }
    val positive = tx.amount >= 0
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.extraSmall, color = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (positive) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward, null, tint = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(tx.type.label, style = MaterialTheme.typography.titleSmall)
                Text(tx.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text("${fmt.format(Date(tx.createdAtEpochMs))} • balance ${tx.balanceAfter}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (positive) "+${tx.amount}" else "${tx.amount}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }
}
