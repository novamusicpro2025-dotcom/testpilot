package com.nova.a12tester.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserProfileScreen(onOpenSettings: () -> Unit, onOpenAdmin: () -> Unit) {
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return
    val completed = state.assignments.count { it.testerId == user.userId && it.status == AssignmentStatus.COMPLETED }
    val submitted = state.apps.count { it.developerId == user.userId }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(76.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(user.displayName.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Column(Modifier.weight(1f)) {
                Text(user.displayName, style = MaterialTheme.typography.titleLarge)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Role: ${user.role.label} • Rep ${user.reputationScore}", style = MaterialTheme.typography.labelMedium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), "Completed", "$completed", Icons.Filled.TaskAlt)
            StatCard(Modifier.weight(1f), "Submitted", "$submitted", Icons.Filled.Folder)
            StatCard(Modifier.weight(1f), "Coins", "${user.coinBalance}", Icons.Filled.MonetizationOn)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Membership", style = MaterialTheme.typography.titleSmall)
                Text("Joined ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(user.createdAtEpochMs))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Status: ${user.accountStatus.label}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings & Account") }
        if (state.isAdmin) OutlinedButton(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.AdminPanelSettings, null); Spacer(Modifier.width(8.dp)); Text("Admin Dashboard") }
        ComplianceNote("Your email and profile image are private and are never shown to other members. Only your display name is visible in testing groups.")
        Spacer(Modifier.height(24.dp))
    }
}
