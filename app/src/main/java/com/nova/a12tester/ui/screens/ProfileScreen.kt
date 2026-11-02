package com.nova.a12tester.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.*

@Composable
fun ProfileScreen(
    vm: TestPilotViewModel,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenHowToUse: () -> Unit,
    onOpenSubmit: () -> Unit = {},
    onOpenMyApps: () -> Unit = {}
) {
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return
    val active = state.assignments.count { it.testerId == user.userId && (it.status == AssignmentStatus.ASSIGNED || it.status == AssignmentStatus.IN_PROGRESS) }
    val completed = state.assignments.count { it.testerId == user.userId && it.status == AssignmentStatus.COMPLETED }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        user.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(user.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    when (user.role) {
                        UserRole.DEVELOPER -> "Developer"
                        UserRole.TESTER -> "Tester"
                        UserRole.BOTH -> "Developer & Tester"
                        UserRole.ADMIN -> "Administrator"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Quick Actions: Submit App, My Apps, and Admin (if admin)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAction("Submit App", Icons.Filled.AddCircle, Modifier.weight(1f), onOpenSubmit)
            QuickAction("My Apps", Icons.Filled.Folder, Modifier.weight(1f), onOpenMyApps)
            if (state.isAdmin) {
                QuickAction("Admin", Icons.Filled.AdminPanelSettings, Modifier.weight(1f), onOpenAdmin)
            }
        }

        // Testing Stats Row: Active Tests, Completed, Pending
        Text(
            "Testing Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Active Tests",
                value = "$active",
                icon = Icons.AutoMirrored.Filled.Assignment,
                accentColor = Color(0xFF4F46E5)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Completed",
                value = "$completed",
                icon = Icons.Filled.TaskAlt,
                accentColor = Color(0xFF10B981)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Pending",
                value = "${user.pendingRewardCoins}",
                icon = Icons.Filled.Schedule,
                accentColor = Color(0xFFF59E0B)
            )
        }

        // Account / Reputation Stats Row
        Text(
            "Account & Rewards",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Reputation",
                value = "${user.reputationScore}",
                icon = Icons.Filled.Star,
                accentColor = Color(0xFF8B5CF6)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Coins",
                value = "${user.coinBalance}",
                icon = Icons.Filled.MonetizationOn,
                accentColor = Color(0xFFF59E0B)
            )
        }

        Button(onClick = onOpenLeaderboard, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.EmojiEvents, null)
            Spacer(Modifier.width(8.dp))
            Text("Top Testers Leaderboard")
        }

        // Role switch chips
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = user.role == UserRole.DEVELOPER || user.role == UserRole.BOTH,
                onClick = { vm.setRole(UserRole.DEVELOPER) },
                label = { Text("Developer") }
            )
            FilterChip(
                selected = user.role == UserRole.TESTER || user.role == UserRole.BOTH,
                onClick = { vm.setRole(UserRole.TESTER) },
                label = { Text("Tester") }
            )
        }

        // Account ID
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Account ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(user.userId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Settings, null)
            Spacer(Modifier.width(8.dp))
            Text("Settings & Account")
        }

        OutlinedButton(onClick = onOpenHowToUse, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Info, null)
            Spacer(Modifier.width(8.dp))
            Text("How to Use (Beginner's Guide)")
        }

        if (state.isAdmin) {
            Button(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AdminPanelSettings, null)
                Spacer(Modifier.width(8.dp))
                Text("Admin Dashboard")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
