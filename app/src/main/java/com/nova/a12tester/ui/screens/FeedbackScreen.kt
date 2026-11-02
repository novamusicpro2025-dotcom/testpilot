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
import com.nova.a12tester.data.FeedbackRequest
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(assignmentId: String, vm: TestPilotViewModel, onDone: () -> Unit) {
    val state by TestPilotRepository.state.collectAsState()
    val a = state.assignments.firstOrNull { it.assignmentId == assignmentId } ?: run {
        EmptyState(Icons.Filled.ErrorOutline, "Assignment not found", ""); return
    }
    var category by remember { mutableStateOf(FeedbackCategory.GENERAL) }
    var severity by remember { mutableStateOf(FeedbackSeverity.MEDIUM) }
    var summary by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var device by remember { mutableStateOf("Android API 34") }
    var screenshotUrl by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }
    var sevExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tester Feedback", style = MaterialTheme.typography.headlineSmall)
        Text("Honest feedback for ${a.appName}. Useful feedback may earn bonus coins.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
            OutlinedTextField(value = category.label, onValueChange = {}, readOnly = true, label = { Text("Category") }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) })
            ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                FeedbackCategory.entries.forEach { c -> DropdownMenuItem(text = { Text(c.label) }, onClick = { category = c; catExpanded = false }) }
            }
        }
        ExposedDropdownMenuBox(expanded = sevExpanded, onExpandedChange = { sevExpanded = it }) {
            OutlinedTextField(value = severity.label, onValueChange = {}, readOnly = true, label = { Text("Severity") }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sevExpanded) })
            ExposedDropdownMenu(expanded = sevExpanded, onDismissRequest = { sevExpanded = false }) {
                FeedbackSeverity.entries.forEach { s -> DropdownMenuItem(text = { Text(s.label) }, onClick = { severity = s; sevExpanded = false }) }
            }
        }
        OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Summary (10-400 chars)") }, placeholder = { Text("Describe the issue or observation") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        OutlinedTextField(value = steps, onValueChange = { steps = it }, label = { Text("Reproduction steps (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        OutlinedTextField(value = device, onValueChange = { device = it }, label = { Text("Device info") }, modifier = Modifier.fillMaxWidth())
        
        OutlinedTextField(
            value = screenshotUrl,
            onValueChange = { screenshotUrl = it },
            label = { Text("Screenshot Proof URL (e.g., Imgur link)") },
            placeholder = { Text("Provide a link to your testing screenshot") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = {
            if (vm.submitFeedback(FeedbackRequest(a.assignmentId, category, severity, summary, steps, device, screenshotUrl))) onDone()
        }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Submit Feedback") }
        TextButton(onClick = onDone) { Text("Back to progress") }
        Spacer(Modifier.height(24.dp))
    }
}
