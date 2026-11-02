package com.nova.a12tester.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nova.a12tester.data.getDailyTesterQuotaStatus
import com.nova.a12tester.data.utcTodayKey
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.*

@Composable
fun MyAppsScreen(
    vm: TestPilotViewModel,
    onOpenApp: (String) -> Unit,
    onSubmitNew: () -> Unit
) {
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return
    val myApps = state.apps.filter { it.developerId == user.userId }.sortedByDescending { it.createdAtEpochMs }

    if (myApps.isEmpty()) {
        EmptyState(
            Icons.Filled.FolderOpen,
            "No submitted apps",
            "Submit your first app to find genuine testers.",
            action = { Button(onSubmitNew) { Text("Submit an App") } }
        )
        return
    }

    LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
        item { SectionHeader("My Submitted Apps", "${myApps.size} total") { TextButton(onSubmitNew) { Text("New") } } }
        items(myApps) { app ->
            val isRemovedDueToQuota = app.moderationStatus == ModerationStatus.SUSPENDED &&
                app.moderationNotes.contains("Daily testing quota not met", ignoreCase = true)

            Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                AppCard(app, onClick = { onOpenApp(app.appId) })

                if (isRemovedDueToQuota) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text("Removed: Daily Testing Quota Missed", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            Text(
                                "Your app was removed from testing because the 10-app daily testing quota was not met. Complete 10 tests today to reactivate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = { vm.reinstateApp(app.appId) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Check Quota & Reactivate App")
                            }
                        }
                    }
                } else {
                    Text(
                        when (app.moderationStatus) {
                            ModerationStatus.PENDING_REVIEW -> "In moderation queue • ${app.assignedTesterCount} assigned"
                            ModerationStatus.APPROVED -> "Active • ${app.assignedTesterCount}/${app.targetTesterCount} testers recruited"
                            ModerationStatus.REJECTED -> "Rejected • refunded. ${app.moderationNotes}"
                            ModerationStatus.CANCELLED -> "Cancelled"
                            ModerationStatus.SUSPENDED -> "Suspended: ${app.moderationNotes}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveAssignmentsScreen(
    vm: TestPilotViewModel,
    onOpenAssignment: (String) -> Unit,
    onFindMoreApps: () -> Unit
) {
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return
    val active = state.assignments.filter {
        it.testerId == user.userId && (it.status == AssignmentStatus.ASSIGNED || it.status == AssignmentStatus.IN_PROGRESS)
    }

    val quota = remember(state.usageSessions, state.apps) {
        TestPilotRepository.getDailyTesterQuotaStatus(user.userId)
    }
    val todayKey = utcTodayKey()

    LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
        item {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                DailyQuotaCard(
                    quota = quota,
                    onFindMoreApps = onFindMoreApps
                )
            }
        }

        item {
            SectionHeader(
                "Active Testing Assignments",
                "${active.size} active • 10 daily tests required"
            )
        }

        if (active.isEmpty()) {
            item {
                EmptyState(
                    Icons.AutoMirrored.Filled.Assignment,
                    "No active tests",
                    "Join testing groups from Discover to test at least 10 apps daily and protect your submitted apps.",
                    action = { Button(onFindMoreApps) { Text("Find Apps to Test") } }
                )
            }
        } else {
            items(active) { a ->
                val session = state.usageSessions.firstOrNull { it.assignmentId == a.assignmentId && it.dateKey == todayKey }
                val targetMet = session?.isTargetMet == true

                AssignmentRow(
                    a = a,
                    onClick = { onOpenAssignment(a.assignmentId) },
                    trailing = {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (targetMet) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (targetMet) Icons.Filled.Check else Icons.Filled.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (targetMet) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    if (targetMet) "2m Done" else "${session?.durationSeconds ?: 0}s/120s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (targetMet) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CompletedTasksScreen() {
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return
    val completed = state.assignments.filter { it.testerId == user.userId && it.status == AssignmentStatus.COMPLETED }
    if (completed.isEmpty()) {
        EmptyState(Icons.Filled.TaskAlt, "No completed tests yet", "Complete a 14-day closed test to earn your reward.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
        item { SectionHeader("Completed Testing Tasks", "${completed.size} completed • rewarded") }
        items(completed) { a -> AssignmentRow(a, onClick = {}) }
    }
}
