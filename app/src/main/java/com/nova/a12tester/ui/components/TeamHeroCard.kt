package com.nova.a12tester.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.*
import com.nova.a12tester.domain.*

/**
 * Hero Card for the Reciprocal Testing Cohort Team (Team 1, Team 2, ...).
 *
 * Rules:
 *  - 200 coins to join.
 *  - Max 13 developers per team.
 *  - Each member installs & tests the other 12 apps daily for 2 minutes for 14 days.
 *  - When 13 members fill, Team 2 is automatically created!
 */
@Composable
fun TeamHeroCard(
    team: TestingCohortTeam,
    currentUser: UserProfile,
    mySubmittedApps: List<SubmittedApp>,
    allAssignments: List<TestingAssignment>,
    usageSessions: List<DailyAppUsageSession>,
    onOpenSubmit: () -> Unit,
    onOpenWallet: () -> Unit,
    onJoinTeam: (appId: String) -> Unit,
    onRecordUsage: (assignmentId: String, seconds: Int) -> Unit,
    onOpenAssignment: (assignmentId: String) -> Unit,
    onOpenApp: (appId: String) -> Unit = {},
    onWatchAd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUserMember = team.members.any { it.userId == currentUser.userId }
    val myMember = team.members.firstOrNull { it.userId == currentUser.userId }
    var selectedAppId by remember(mySubmittedApps) {
        mutableStateOf(mySubmittedApps.firstOrNull()?.appId.orEmpty())
    }
    val joinCost = team.joinCostCoins // 200 coins
    val hasEnoughCoins = currentUser.coinBalance >= joinCost
    val todayKey = utcTodayKey()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 12 peer apps for user if joined
    val peerMembers = team.members.filter { it.userId != currentUser.userId }
    val peerAssignments = allAssignments.filter { a ->
        a.testerId == currentUser.userId && peerMembers.any { it.appId == a.appId }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Hero Gradient Header
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF4F46E5), // Electric Indigo
                                Color(0xFF2563EB), // Royal Blue
                                Color(0xFF06B6D4), // Cyan
                                Color(0xFF10B981)  // Emerald
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Text(
                                team.teamName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (team.isFull) Color(0xFF10B981) else Color(0xFFF59E0B)
                        ) {
                            Text(
                                if (team.isFull) "Active • 14 Days" else "Recruiting (${team.members.size}/13)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        "13 Developers Closed Testing Cohort",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Text(
                        "Each developer tests the remaining 12 apps daily for 2 minutes for 14 days so every app gets 12 Google Play testers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(Modifier.height(4.dp))
                    // Progress bar
                    LinearProgressIndicator(
                        progress = { team.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${team.members.size} / 13 Developers Joined",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (team.isFull) "Cohort Full • In Testing" else "${team.remainingSlots} slots open",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Member Avatars Row (13 slots visualizer)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Team Cohort Roster (13 Slots):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (0 until team.maxMembers).forEach { slotIndex ->
                        val member = team.members.getOrNull(slotIndex)
                        if (member != null) {
                            val isMe = member.userId == currentUser.userId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = if (isMe) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.width(76.dp)
                            ) {
                                Column(
                                    Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isMe) MaterialTheme.colorScheme.primary else Color(0xFF3B82F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (isMe) "YOU" else member.appName.take(2).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        if (isMe) "Your App" else member.appName.take(8),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                    Text(
                                        "Slot #${slotIndex + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Empty slot
                            Box(
                                Modifier
                                    .width(76.dp)
                                    .height(86.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                                    Text(
                                        "Slot ${slotIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        "Open",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Action Area: Joined vs Not Joined
                if (!isUserMember) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Entry: 200 Coins  •  Your Balance: ${currentUser.coinBalance} Coins",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (mySubmittedApps.isEmpty()) {
                            // User hasn't submitted an app yet
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f)) {
                                        Text("Submit Your App First (300 Coins)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text("To join ${team.teamName}, submit your app details and icon so the other 12 developers can test it.", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            Button(
                                onClick = onOpenSubmit,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Filled.AddCircle, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Submit App Details (300 Coins)")
                            }
                        } else {
                            // User has app(s) submitted
                            if (mySubmittedApps.size > 1) {
                                Text("Select which app to enroll into ${team.teamName}:", style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    mySubmittedApps.forEach { app ->
                                        FilterChip(
                                            selected = selectedAppId == app.appId,
                                            onClick = { selectedAppId = app.appId },
                                            label = { Text(app.appName) }
                                        )
                                    }
                                }
                            }

                            Button(
                                enabled = hasEnoughCoins && selectedAppId.isNotBlank(),
                                onClick = { onJoinTeam(selectedAppId) },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Filled.GroupAdd, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (hasEnoughCoins) "Join ${team.teamName} (200 Coins)" else "Need 200 Coins")
                            }
                        }
                    }
                } else {
                    // User IS in the team!
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Column {
                                    Text("You are an active member of ${team.teamName}! 🎉", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Your app '${myMember?.appName.orEmpty()}' is in 14-day closed testing by 12 developers.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Text(
                            "Your Testing Duty: Remaining 12 Peer Apps (2 min daily):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // 12 peer apps list
                        if (peerMembers.isEmpty()) {
                            Text("Waiting for more developers to join ${team.teamName}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                peerMembers.forEach { peer ->
                                    val assignment = peerAssignments.firstOrNull { it.appId == peer.appId }
                                    val session = usageSessions.firstOrNull { it.appId == peer.appId && it.dateKey == todayKey }
                                    val isTestedToday = session?.isTargetMet == true
                                    val durSeconds = session?.durationSeconds ?: 0
                                    val launchIntent = remember(peer.packageName) {
                                        context.packageManager.getLaunchIntentForPackage(peer.packageName)
                                    }
                                    val isInstalled = launchIntent != null

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isTestedToday) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val aId = assignment?.assignmentId ?: "${peer.appId}_${currentUser.userId}"
                                                if (!isInstalled) {
                                                    onOpenApp(peer.appId)
                                                } else {
                                                    try {
                                                        context.startActivity(launchIntent)
                                                    } catch (_: Exception) {}
                                                    onOpenAssignment(aId)
                                                }
                                            }
                                    ) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isTestedToday) Color(0xFF10B981) else if (isInstalled) Color(0xFF2563EB) else Color(0xFFF59E0B)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    peer.appName.take(2).uppercase(),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }

                                            Column(Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(peer.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (isInstalled) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            if (isInstalled) "Installed ✓" else "Not Installed",
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isInstalled) Color(0xFF10B981) else Color(0xFFD97706),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Text(
                                                    "by ${peer.userDisplayName} • 14-Day Cycle",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    LinearProgressIndicator(
                                                        progress = { (durSeconds.toFloat() / 120f).coerceIn(0f, 1f) },
                                                        modifier = Modifier
                                                            .width(70.dp)
                                                            .height(5.dp)
                                                            .clip(RoundedCornerShape(3.dp)),
                                                        color = if (isTestedToday) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        if (isTestedToday) "Tested 2m ✓" else "$durSeconds/120s",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (isTestedToday) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            val aId = assignment?.assignmentId ?: "${peer.appId}_${currentUser.userId}"

                                            if (!isInstalled) {
                                                Button(
                                                    onClick = { onOpenApp(peer.appId) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Filled.GetApp, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Install", color = Color.White, fontWeight = FontWeight.SemiBold)
                                                }
                                            } else if (!isTestedToday) {
                                                Button(
                                                    onClick = {
                                                        try {
                                                            context.startActivity(launchIntent)
                                                        } catch (_: Exception) {}
                                                        onOpenAssignment(aId)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Test 2m", color = Color.White, fontWeight = FontWeight.SemiBold)
                                                }
                                            } else {
                                                FilledTonalButton(
                                                    onClick = {
                                                        try {
                                                            context.startActivity(launchIntent)
                                                        } catch (_: Exception) {
                                                            onOpenAssignment(aId)
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp), tint = Color(0xFF10B981))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Done", color = Color(0xFF10B981))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
