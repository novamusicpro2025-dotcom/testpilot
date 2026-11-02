package com.nova.a12tester.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.*

@Composable
fun TestingInstructionsScreen(assignmentId: String, vm: TestPilotViewModel, onDone: () -> Unit) {
    val state by TestPilotRepository.state.collectAsState()
    val a = state.assignments.firstOrNull { it.assignmentId == assignmentId } ?: run {
        EmptyState(Icons.Filled.ErrorOutline, "Assignment not found", "Return to your assignments."); return
    }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Testing Instructions", style = MaterialTheme.typography.headlineSmall)
        Text(a.appName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text("Developer: ${a.developerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        StepCard(step = 1, title = "Join the Google Group", body = "Open the Google Group and request to join the closed testers group.", icon = Icons.Filled.Group, done = a.groupJoinedConfirmed, onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.googleGroupUrl))) }, onConfirm = { vm.confirmGroupJoined(a.assignmentId) })
        StepCard(step = 2, title = "Opt-in on Google Play", body = "Open the Play Store testing link and accept becoming a tester.", icon = Icons.AutoMirrored.Filled.OpenInNew, done = a.optInConfirmed, onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.optInUrl))) }, onConfirm = { vm.confirmOptedIn(a.assignmentId) })
        StepCard(step = 3, title = "Install the App", body = "Install the app from the Play Store once opt-in is accepted.", icon = Icons.Filled.GetApp, done = a.installConfirmed, onClick = { vm.confirmInstalled(a.assignmentId) })

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Developer's Instructions", style = MaterialTheme.typography.titleSmall)
                Text(a.testingInstructions, style = MaterialTheme.typography.bodyMedium)
            }
        }

        ComplianceNote("Group join, opt-in and install are self-reported by you. TestPilot cannot verify these through any public Google API. Do not use automated tools to join groups or interact with the Play Store.")
        Spacer(Modifier.height(4.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Continue to Testing Progress") }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StepCard(step: Int, title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector, done: Boolean, onClick: () -> Unit, onConfirm: (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth(), colors = if (done) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) else CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = MaterialTheme.shapes.small, color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (done) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary) else Text("$step", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClick) { Text(if (step == 3) "Mark Installed" else "Open Link") }
                if (onConfirm != null) {
                    OutlinedButton(onClick = onConfirm, enabled = !done) { Text("I Completed This") }
                }
            }
        }
    }
}
