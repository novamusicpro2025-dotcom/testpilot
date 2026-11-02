package com.nova.a12tester.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.*
import com.nova.a12tester.ui.theme.isAppInDarkTheme

@Composable
fun AppDetailsScreen(
    appId: String,
    vm: TestPilotViewModel,
    onOpenProgress: (String) -> Unit,
    onOpenChat: ((appId: String, assignmentId: String, receiverId: String) -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return
    val app = state.apps.firstOrNull { it.appId == appId } ?: run {
        state.cohortTeams.flatMap { it.members }.firstOrNull { it.appId == appId }?.let { m ->
            SubmittedApp(
                appId = m.appId,
                developerId = m.userId,
                developerName = m.userDisplayName,
                appName = m.appName,
                packageName = m.packageName,
                iconUrl = m.appIconUrl,
                shortDescription = "Team 1 closed-testing peer app.",
                optInUrl = m.optInUrl,
                googleGroupUrl = m.googleGroupUrl,
                privacyPolicyUrl = "",
                minAndroidVersion = 24,
                testingInstructions = "1. Join the Google Group using the link below.\n2. Open the Play Store opt-in link and accept becoming a tester.\n3. Install the app and follow the developer instructions below.\n\nOpen and test daily for 2 minutes to keep reciprocal testing active.",
                testingDurationDays = 14,
                submissionCostCoins = 100,
                rewardPerTesterCoins = 25,
                moderationStatus = ModerationStatus.APPROVED,
                targetTesterCount = 12,
                assignedTesterCount = 1,
                createdAtEpochMs = m.joinedAtEpochMs
            )
        }
    }
    if (app == null) { EmptyState(Icons.Filled.ErrorOutline, "App not found", "This app may have been removed."); return }
    val myAssignment = state.assignments.firstOrNull { it.appId == appId && it.testerId == user.userId }
    val context = LocalContext.current
    var showRewardDialog by remember { mutableStateOf(false) }
    var rewardedAmount by remember { mutableStateOf(0) }

    if (showRewardDialog) {
        val assignedId = myAssignment?.assignmentId ?: "${app.appId}_${user.userId}"
        AlertDialog(
            onDismissRequest = { showRewardDialog = false },
            icon = {
                Icon(
                    Icons.Filled.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "🎉 +$rewardedAmount Coins Credited!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "You received +$rewardedAmount coins for installing ${app.appName} (${if (app.isVipTopPinned) "3-Month VIP Top Spotlight" else if (app.isFeaturedPremium) "1-Month PRO Featured" else "Standard"} App).\n\nKeep the app installed and test daily for 2 minutes for 14 days to complete reciprocal closed testing!",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRewardDialog = false
                        onOpenProgress(assignedId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Start 2-Min Daily Test", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRewardDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                modifier = Modifier.size(76.dp),
                shadowElevation = 4.dp
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF6366F1), // Indigo
                                    Color(0xFF06B6D4)  // Cyan
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        app.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(app.developerName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                ModerationStatusChip(app.moderationStatus)
                if (app.isVipTopPinned) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE), shape = MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("👑", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "#1 VIP TOP SPOTLIGHT (+70 COINS) • 🛡️ PLAY STORE GUARANTEED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
                        )
                    }
                } else if (app.isFeaturedPremium) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(if (isDark) Color(0xFF382307) else Color(0xFFFEF3C7), shape = MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("⭐", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "PRO FEATURED APP (+50 COINS)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                        )
                    }
                } else {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9), shape = MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("📱", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "COMMUNITY RECIPROCAL TEST (+25 COINS)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        )
                    }
                }
            }
        }

        Text(app.shortDescription, style = MaterialTheme.typography.bodyLarge)

        val rewardCoins = if (app.isVipTopPinned) 70 else if (app.isFeaturedPremium) 50 else (if (app.rewardPerTesterCoins > 0) app.rewardPerTesterCoins else 25)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "Tester Reward", "+$rewardCoins", Icons.Filled.MonetizationOn, accentColor = Color(0xFFF59E0B))
            StatCard(Modifier.weight(1f), "Duration", "${app.testingDurationDays}d", Icons.Filled.Event, accentColor = Color(0xFF4F46E5))
            StatCard(Modifier.weight(1f), "Min SDK", "API ${app.minAndroidVersion}", Icons.Filled.Android, accentColor = Color(0xFF10B981))
        }

        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFEEF2FF).copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Assignment, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(22.dp))
                    Text("Testing Instructions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Color(0xFF2563EB), modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("1", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    }
                    Text("Join the Google Group using the link below.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Color(0xFF0F9D58), modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("2", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    }
                    Text("Open the Play Store opt-in link and accept becoming a tester.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Color(0xFF7C3AED), modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("3", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    }
                    Text("Install the app and test for 2 minutes daily for 14 days.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }

                if (app.testingInstructions.isNotBlank() && !app.testingInstructions.startsWith("1. Join the Google Group")) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(app.testingInstructions, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Button(
            onClick = {
                try {
                    val url = ValidationEngine.cleanGoogleGroupUrl(app.googleGroupUrl)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {}
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Filled.Group, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Open Google Group", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.optInUrl)))
                } catch (_: Exception) {}
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Open Play Opt-In Link", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        if (onOpenChat != null && app.developerId != user.userId) {
            val assignedId = myAssignment?.assignmentId ?: "${app.appId}_${user.userId}"
            OutlinedButton(
                onClick = { onOpenChat(app.appId, assignedId, app.developerId) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, null)
                Spacer(Modifier.width(8.dp))
                Text("Chat with Developer", fontWeight = FontWeight.SemiBold)
            }
        }

        ComplianceNote("TestPilot cannot verify Google Group membership or Play Store testing eligibility; complete these yourself through the official links. Developers remain responsible for meeting current Google Play testing requirements.")

        if (app.developerId == user.userId) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(Modifier.padding(16.dp)) {
                    Text("Your Submission", style = MaterialTheme.typography.titleSmall)
                    Text("Assigned testers: ${app.assignedTesterCount} / ${app.targetTesterCount}", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            val launchIntent = remember(app.packageName) {
                context.packageManager.getLaunchIntentForPackage(app.packageName)
            }
            val isInstalled = launchIntent != null
            val assignmentId = myAssignment?.assignmentId ?: "${app.appId}_${user.userId}"

            val isInstallConfirmed = myAssignment?.installConfirmed == true

            if (isInstallConfirmed) {
                Button(
                    onClick = {
                        try {
                            if (launchIntent != null) context.startActivity(launchIntent)
                        } catch (_: Exception) {}
                        onOpenProgress(assignmentId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("App Installed ✓ (+$rewardCoins Coins Credited) — Test Now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                val buttonColor = when {
                    app.isVipTopPinned -> Color(0xFF7C3AED)
                    app.isFeaturedPremium -> Color(0xFFF59E0B)
                    else -> Color(0xFF2563EB)
                }

                Button(
                    onClick = {
                        val coins = vm.installAndRewardApp(app.appId)
                        rewardedAmount = if (coins > 0) coins else rewardCoins
                        showRewardDialog = true
                        try {
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            } else {
                                val url = if (app.optInUrl.isNotBlank()) app.optInUrl else "https://play.google.com/store/apps/details?id=${app.packageName}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Filled.GetApp, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Install App & Claim +$rewardCoins Coins",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
