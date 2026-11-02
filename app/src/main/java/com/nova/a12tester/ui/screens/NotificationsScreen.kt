package com.nova.a12tester.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    vm: TestPilotViewModel,
    onNavigate: (String) -> Unit
) {
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return
    val myNotifs = state.notifications.filter { it.userId == user.userId }.sortedByDescending { it.createdAtEpochMs }
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    if (myNotifs.isEmpty()) {
        EmptyState(Icons.Filled.NotificationsOff, "No notifications", "You're all caught up.")
        return
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { vm.markAllRead() }) { Text("Mark all read") }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(myNotifs) { n ->
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clickable { 
                        vm.markNotifRead(n.notificationId)
                        if (n.deepLinkRoute.isNotBlank()) {
                            onNavigate(n.deepLinkRoute)
                        }
                    },
                    colors = if (!n.isRead) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) else CardDefaults.cardColors(),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            when (n.type) {
                                NotificationType.REWARD -> Icons.Filled.CardGiftcard
                                NotificationType.ASSIGNMENT -> Icons.AutoMirrored.Filled.Assignment
                                NotificationType.MODERATION -> Icons.Filled.Gavel
                                NotificationType.FEEDBACK -> Icons.AutoMirrored.Filled.Comment
                                NotificationType.SYSTEM -> Icons.Filled.Info
                                NotificationType.DISPUTE -> Icons.Filled.Warning
                            }, null, tint = MaterialTheme.colorScheme.primary
                        )
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(n.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                if (!n.isRead) Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary))
                            }
                            Text(n.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(fmt.format(Date(n.createdAtEpochMs)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
