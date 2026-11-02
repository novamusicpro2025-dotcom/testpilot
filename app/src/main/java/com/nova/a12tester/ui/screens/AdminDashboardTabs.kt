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
import java.text.SimpleDateFormat
import java.util.*

@Composable
internal fun GroupsTab(vm: TestPilotViewModel) {
    val state by TestPilotRepository.state.collectAsState()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionHeader("Testing Groups", "${state.groups.size} groups") }
        items(state.groups) { g ->
            val active = state.assignments.filter { it.groupId == g.groupId && it.status != AssignmentStatus.COMPLETED && it.status != AssignmentStatus.REPLACED }
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(g.appName, style = MaterialTheme.typography.titleSmall)
                    Text("${g.currentCount}/${g.maxParticipants} participants • ${g.status.label}", style = MaterialTheme.typography.bodySmall)
                    if (active.isNotEmpty()) {
                        Text("Possible inactive testers:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                        active.forEach { a ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${a.testerDisplayName} • ${a.reportedDaysCount}/${a.requiredDays}d", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                TextButton(onClick = { vm.replaceTester(a.assignmentId) }) { Text("Replace") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReportsTab(vm: TestPilotViewModel) {
    val state by TestPilotRepository.state.collectAsState()
    val open = state.reports.filter { it.status == ReportStatus.OPEN }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionHeader("Abuse Reports", "${open.size} open") }
        if (open.isEmpty()) item { EmptyState(Icons.Filled.Flag, "No open reports", "All reports handled.") }
        items(open) { r ->
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${r.reason.label} — ${r.targetAppName}", style = MaterialTheme.typography.titleSmall)
                    Text(r.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.resolveReport(r.reportId, true, "App suspended") }) { Text("Remove App") }
                        OutlinedButton(onClick = { vm.resolveReport(r.reportId, false, "Dismissed as invalid") }) { Text("Dismiss") }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LedgerTab() {
    val state by TestPilotRepository.state.collectAsState()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { SectionHeader("Coin Ledger", "${state.transactions.size} transactions") }
        items(state.transactions.sortedByDescending { it.createdAtEpochMs }) { tx -> TransactionRow(tx) }
    }
}

@Composable
internal fun AuditTab() {
    val state by TestPilotRepository.state.collectAsState()
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { SectionHeader("Audit Logs", "${state.auditLogs.size} entries") }
        items(state.auditLogs.sortedByDescending { it.createdAtEpochMs }) { log ->
            Card(shape = MaterialTheme.shapes.small) {
                Column(Modifier.padding(12.dp)) {
                    Text(log.action, style = MaterialTheme.typography.titleSmall)
                    Text(log.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("by ${log.adminEmail} • ${fmt.format(Date(log.createdAtEpochMs))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
