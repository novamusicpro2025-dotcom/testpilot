package com.nova.a12tester.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.*

/**
 * Admin dashboard. Access is gated by the server-verified admin custom claim
 * (isAdminClaimVerified). UI hiding is defense-in-depth only; the Cloud Functions and
 * Firestore rules independently enforce authorization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(vm: TestPilotViewModel) {
    val state by TestPilotRepository.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Moderation", "Users", "Groups", "Reports", "Ledger", "Audit")

    if (!state.isAdmin) {
        EmptyState(Icons.Filled.Lock, "Administrator access required", "This area is protected by server-side authorization. Your account is not an admin.")
        return
    }

    Column(Modifier.fillMaxSize()) {
        Text("Admin Dashboard", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(20.dp))
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
            tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> ModerationTab(vm)
            1 -> UsersTab(vm)
            2 -> GroupsTab(vm)
            3 -> ReportsTab(vm)
            4 -> LedgerTab()
            5 -> AuditTab()
        }
    }
}

@Composable
private fun ModerationTab(vm: TestPilotViewModel) {
    val state by TestPilotRepository.state.collectAsState()
    val pending = state.apps.filter { it.moderationStatus == ModerationStatus.PENDING_REVIEW }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Moderation Queue", "${pending.size} awaiting review") }
        if (pending.isEmpty()) item { EmptyState(Icons.Filled.Verified, "Queue clear", "No apps awaiting moderation.") }
        items(pending) { app ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(app.appName, style = MaterialTheme.typography.titleSmall)
                    Text("${app.developerName} • ${app.packageName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(app.optInUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    Text(app.googleGroupUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.approve(app.appId) }) { Text("Approve") }
                        OutlinedButton(onClick = { vm.reject(app.appId, "Fails manual policy/URL review") }) { Text("Reject & Refund") }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersTab(vm: TestPilotViewModel) {
    val state by TestPilotRepository.state.collectAsState()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("User Management", "${state.users.size} accounts") }
        items(state.users) { u ->
            Card(shape = MaterialTheme.shapes.small) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(u.displayName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        AccountStatusChip(u.accountStatus)
                    }
                    Text("${u.email} • ${u.coinBalance} coins • role: ${u.role.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (u.accountStatus == AccountStatus.ACTIVE) TextButton(onClick = { vm.suspendUser(u.userId, "Manual admin suspension") }) { Text("Suspend") }
                        else TextButton(onClick = { vm.restoreUser(u.userId) }) { Text("Restore") }
                        OutlinedButton(onClick = { vm.adjustCoins(u.userId, 10000, "Admin top-up +10,000 coins") }) { Text("+10k Coins") }
                    }
                }
            }
        }
    }
}
