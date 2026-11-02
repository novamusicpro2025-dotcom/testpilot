package com.nova.a12tester.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.data.utcTodayKey
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.*

@Composable
fun TestingProgressScreen(
    assignmentId: String,
    vm: TestPilotViewModel,
    onOpenFeedback: (String) -> Unit,
    onOpenChat: ((appId: String, assignmentId: String, developerId: String) -> Unit)? = null
) {
    val state by TestPilotRepository.state.collectAsState()
    val a = state.assignments.firstOrNull { it.assignmentId == assignmentId } ?: run {
        EmptyState(Icons.Filled.ErrorOutline, "Assignment not found", "Return to your assignments."); return
    }
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }
    var screenshotUrl by remember { mutableStateOf("") }

    val launchIntent = remember(a.packageName) {
        context.packageManager.getLaunchIntentForPackage(a.packageName)
    }
    val isInstalled = launchIntent != null
    val todayKey = utcTodayKey()
    val todaySession = state.usageSessions.firstOrNull { it.assignmentId == a.assignmentId && it.dateKey == todayKey }
    val isTestedToday = todaySession?.isTargetMet == true

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                a.appName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            AssignmentStatusChip(a.status)
        }
        Text(
            "Developer: ${a.developerName} • Package: ${a.packageName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LinearProgressIndicator(progress = { a.progressFraction }, modifier = Modifier.fillMaxWidth().height(8.dp))
        Text(
            "${a.reportedDaysCount} of ${a.requiredDays} days reported • ${a.remainingDays} days remaining",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "Check-Ins", "${a.verifiedCheckInsCount}", Icons.Filled.EventAvailable, accentColor = Color(0xFF4F46E5))
            StatCard(Modifier.weight(1f), "Daily Test", if (isTestedToday) "2m Done ✓" else "${todaySession?.durationSeconds ?: 0}s/120s", Icons.Filled.Timer, accentColor = if (isTestedToday) Color(0xFF10B981) else Color(0xFFF59E0B))
            StatCard(Modifier.weight(1f), "Status", if (isInstalled) "Installed ✓" else "Need Install", Icons.Filled.GetApp, accentColor = if (isInstalled) Color(0xFF10B981) else Color(0xFF2563EB))
        }

        if (onOpenChat != null) {
            Button(
                onClick = { onOpenChat(a.appId, a.assignmentId, a.developerId) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Chat with Developer (${a.developerName})", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Setup & Installation Steps (Google Group -> Play Opt-In -> Install)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Setup & Installation Steps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "You must join the Google Group and opt-in on Google Play to install and test this closed testing app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Step 1: Join Google Group
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        if (a.groupJoinedConfirmed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (a.groupJoinedConfirmed) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("1. Join Google Group", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Authorizes your Google account for closed testing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            try {
                                val url = com.nova.a12tester.domain.ValidationEngine.cleanGoogleGroupUrl(a.googleGroupUrl)
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) {}
                            vm.confirmGroupJoined(a.assignmentId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(if (a.groupJoinedConfirmed) "Reopen" else "Join Group", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Step 2: Opt-In on Google Play
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        if (a.optInConfirmed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (a.optInConfirmed) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("2. Opt-in on Google Play", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Accept becoming a closed tester on Play Store web link", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.optInUrl)))
                            } catch (_: Exception) {}
                            vm.confirmOptedIn(a.assignmentId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(if (a.optInConfirmed) "Reopen" else "Opt-in Link", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Step 3: Install App
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        if (isInstalled || a.installConfirmed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isInstalled || a.installConfirmed) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("3. Install App on Device", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (isInstalled) "Detected on your phone ✓" else "Not detected on phone yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isInstalled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${a.packageName}")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(marketIntent)
                            } catch (_: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${a.packageName}")))
                            }
                            vm.confirmInstalled(a.assignmentId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.GetApp, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text(if (isInstalled) "Store" else "Install", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 14-Day Daily Usage Reminder (shown after install)
        if (isInstalled && !isTestedToday) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Use this app daily for 14 days",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "Open ${a.appName} and test for at least 2 minutes every day for 14 consecutive days to complete your closed testing assignment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                try { context.startActivity(launchIntent) } catch (_: Exception) {}
                            }
                        ) { Text("Open App") }
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.optInUrl)))
                                } catch (_: Exception) {}
                            }
                        ) { Text("Opt-In Link") }
                    }
                }
            }
        }

        // Daily 2-Minute Usage Test Card
        AppTestUsageCard(
            appName = a.appName,
            session = todaySession,
            onAddSeconds = { secs -> vm.recordUsage(a.assignmentId, secs) },
            onLaunchApp = {
                if (launchIntent != null) {
                    try {
                        context.startActivity(launchIntent)
                    } catch (_: Exception) {}
                } else {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.optInUrl)))
                    } catch (_: Exception) {}
                }
            },
            onSyncNow = {
                com.nova.a12tester.data.UsageStatsHelper.syncRealDeviceUsage(context)
            }
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            label = { Text("Today's testing note (optional)") },
            placeholder = { Text("What did you test today?") }
        )

        OutlinedTextField(
            value = screenshotUrl,
            onValueChange = { screenshotUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Screenshot proof URL (optional)") },
            placeholder = { Text("https://imgur.com/...") }
        )

        Button(
            onClick = { if (vm.dailyCheckIn(a.assignmentId, note, screenshotUrl)) { note = ""; screenshotUrl = "" } },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = a.reportedDaysCount < a.requiredDays && a.status != AssignmentStatus.COMPLETED
        ) {
            Text("Record Daily Check-In (1 per day, UTC)")
        }

        if (a.installationProblemReported) {
            ErrorState("You reported an installation issue. Details: ${a.installationProblemDetails}")
        } else {
            TextButton(onClick = { vm.reportInstallIssue(a.assignmentId, "Reported via progress screen") }) {
                Text("Report an installation problem")
            }
        }

        Button(
            onClick = { onOpenFeedback(a.assignmentId) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !a.feedbackSubmitted
        ) {
            Text(if (a.feedbackSubmitted) "Feedback Submitted ✓" else "Submit Tester Feedback")
        }
        
        if (onOpenChat != null) {
            OutlinedButton(
                onClick = { onOpenChat(a.appId, a.assignmentId, a.developerId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Chat with Developer")
            }
        }

        ComplianceNote("Closed testing requires 14 consecutive days of testing with genuine daily usage. TestPilot enforces reciprocal testing rules so every developer receives 12 testers.")
    }
}
