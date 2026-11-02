package com.nova.a12tester.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportAppScreen(appId: String, vm: TestPilotViewModel, onDone: () -> Unit) {
    val state by TestPilotRepository.state.collectAsState()
    val app = state.apps.firstOrNull { it.appId == appId }
    var reason by remember { mutableStateOf(ReportReason.POLICY_VIOLATION) }
    var details by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Report App", style = MaterialTheme.typography.headlineSmall)
        Text(app?.appName ?: "App", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text("Help keep the community safe. Reports go to moderators for review.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(value = reason.label, onValueChange = {}, readOnly = true, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ReportReason.entries.forEach { r -> DropdownMenuItem(text = { Text(r.label) }, onClick = { reason = r; expanded = false }) }
            }
        }
        OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details (min 10 chars)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Button(onClick = { if (vm.reportApp(appId, reason, details)) onDone() }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Submit Report") }
        TextButton(onClick = onDone) { Text("Cancel") }
        Spacer(Modifier.height(24.dp))
    }
}
