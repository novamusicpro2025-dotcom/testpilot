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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.*
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.*
import com.nova.a12tester.ui.theme.BrandAmber
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.nova.a12tester.data.AdMobManager
import com.nova.a12tester.ui.theme.BrandIndigo
import com.nova.a12tester.ui.theme.BrandTealDark
import com.nova.a12tester.ui.theme.isAppInDarkTheme

@Composable
fun HomeScreen(
    vm: TestPilotViewModel,
    onOpenDiscover: () -> Unit,
    onOpenSubmit: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAssignment: (String) -> Unit,
    onOpenMyApps: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenApp: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return
    val isDark = isAppInDarkTheme()
    val todayKey = utcTodayKey()
    var rechargeMessage by remember { mutableStateOf<String?>(null) }
    val todaySessions = state.usageSessions.filter { it.testerId == user.userId && it.dateKey == todayKey }
    val myAssignments = state.assignments.filter { it.testerId == user.userId }
    val active = myAssignments.filter { it.status == AssignmentStatus.ASSIGNED || it.status == AssignmentStatus.IN_PROGRESS }
    val unreadNotifs = state.notifications.count { it.userId == user.userId && !it.isRead }
    
    // Find which team the current user is in
    val myTeam = state.cohortTeams.firstOrNull { t -> t.members.any { it.userId == user.userId } }
    
    val submittedApps = remember(state.apps, myTeam) {
        val approvedApps = state.apps.filter { it.moderationStatus == ModerationStatus.APPROVED }
        
        // Show premium/VIP apps OR apps that belong to developers in the same team as the current user
        val visibleApps = approvedApps.filter { app ->
            app.isFeaturedPremium || app.isVipTopPinned || 
            (myTeam != null && myTeam.members.any { member -> member.userId == app.developerId })
        }
        
        visibleApps.sortedWith(
            compareByDescending<SubmittedApp> { it.pinPriority }
                .thenByDescending { it.createdAtEpochMs }
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hello, ${user.displayName.substringBefore(' ')}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (user.role == UserRole.DEVELOPER) "Developer workspace" else "Tester workspace", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BadgedBox(badge = { if (unreadNotifs > 0) Badge { Text("$unreadNotifs") } }) {
                    IconButton(onClick = onOpenNotifications) { Icon(Icons.Filled.Notifications, "Notifications") }
                }
            }
        }
        // ─── Recharge (Top Up Wallet) ─────────────────────────
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onOpenWallet() }
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Recharge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Tap here to view plans and pay via Google Play", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go to Wallet")
                    }
                }
            }
        }

        item {
            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    if (isDark) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFFFEF3C7).copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.MonetizationOn,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column {
                                Text("Coins Balance", style = MaterialTheme.typography.labelSmall, color = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F))
                                Text("${user.coinBalance}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309))
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenWallet,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Text("Wallet", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (user.isVipActive) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1E1435) else Color(0xFFEDE9FE),
                            border = BorderStroke(1.2.dp, if (isDark) Color(0xFFA78BFA) else Color(0xFF8B5CF6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Verified, null, tint = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED), modifier = Modifier.size(22.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("👑 3-Month VIP Active (Ad-Free)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFEDE9FE) else Color(0xFF5B21B6))
                                    Text("${user.premiumDaysRemaining} days remaining • #1 Top Pinned Spotlight • 🛡️ Play Store Guaranteed", style = MaterialTheme.typography.labelSmall, color = if (isDark) Color(0xFFDDD6FE) else Color(0xFF6D28D9))
                                }
                            }
                        }
                    } else if (user.isPremiumActive) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1F1A12) else Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Verified, null, tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706), modifier = Modifier.size(22.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("👑 PRO Ad-Free Active", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF92400E))
                                    Text("${user.premiumDaysRemaining} days remaining • Apps pinned on Home Screen", style = MaterialTheme.typography.labelSmall, color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309))
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (activity != null) {
                                    AdMobManager.showRewardedAd(
                                        activity = activity,
                                        onUserEarnedReward = {
                                            TestPilotRepository.creditRewardedAdCompletion(java.util.UUID.randomUUID().toString())
                                        },
                                        onAdFailed = {
                                            onOpenWallet()
                                        }
                                    )
                                } else {
                                    onOpenWallet()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Filled.Slideshow, null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Watch Ad (+15 Coins)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(14.dp)) }

        // Usage Access permission prompt if not granted
        item {
            Box(Modifier.padding(horizontal = 20.dp)) {
                UsageAccessBanner()
            }
        }
        item { Spacer(Modifier.height(12.dp)) }

        // ─── Active Testing Assignments ─────────────────────────────────────
        item {
            SectionHeader("Active Testing Assignments", "Your current closed tests (${active.size})")
        }
        if (active.isEmpty()) {
            item {
                EmptyState(
                    Icons.Filled.SearchOff,
                    "No active assignments",
                    "Join a cohort team above or tap any submitted app below to start testing and earning coins.",
                    action = {
                        Button(onClick = onOpenSubmit) {
                            Text("Submit your app")
                        }
                    }
                )
            }
        } else {
            items(active) { a ->
                val session = todaySessions.firstOrNull { it.assignmentId == a.assignmentId }
                val alreadyDone = session?.isTargetMet == true
                AssignmentRow(
                    a = a,
                    onClick = { onOpenAssignment(a.assignmentId) },
                    trailing = {
                        if (alreadyDone) {
                            Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Button(
                                onClick = {
                                    val ok = vm.recordUsage(a.assignmentId, 120)
                                    rechargeMessage = if (ok) "✅ Checked in +2 min on ${a.appName}" else "⚠️ Could not log check-in"
                                },
                                modifier = Modifier.padding(top = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text("+2m", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            }
        }

        item { Spacer(Modifier.height(14.dp)) }

        // ─── Submitted Apps For Testing (One by One) ────────────────────────
        item {
            SectionHeader(
                "Submitted Apps for Testing",
                "${submittedApps.size} apps available • VIP Spotlight (+70 Coins) • PRO Apps (+50 Coins)"
            )
        }

        if (submittedApps.isEmpty()) {
            item {
                EmptyState(
                    Icons.Filled.FolderOpen,
                    "No apps submitted yet",
                    "Be the first developer to submit your app for 14-day reciprocal testing.",
                    action = {
                        Button(onClick = onOpenSubmit) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Submit App Now")
                        }
                    }
                )
            }
        } else {
            items(submittedApps) { app ->
                Box(Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                    AppCard(
                        app = app,
                        onClick = { onOpenApp(app.appId) },
                        trailing = {
                            val reward = if (app.isVipTopPinned) 70 else if (app.isFeaturedPremium) 50 else (if (app.rewardPerTesterCoins > 0) app.rewardPerTesterCoins else 25)
                            Button(
                                onClick = { onOpenApp(app.appId) },
                                shape = RoundedCornerShape(10.dp),
                                colors = when {
                                    app.isVipTopPinned -> ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                                    app.isFeaturedPremium -> ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                                    else -> ButtonDefaults.buttonColors()
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Test (+$reward)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    )
                }
            }
        }
    }
}


