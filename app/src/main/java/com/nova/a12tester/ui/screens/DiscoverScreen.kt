package com.nova.a12tester.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(vm: TestPilotViewModel, onOpenApp: (String) -> Unit) {
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return
    var query by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("All") }
    val categories = listOf("All") + state.apps.map { it.category }.distinct()

    val myAssignmentAppIds = state.assignments.filter { it.testerId == user.userId }.map { it.appId }.toSet()
    val results = state.apps.filter { app ->
        app.moderationStatus == ModerationStatus.APPROVED &&
            app.developerId != user.userId &&
            (categoryFilter == "All" || app.category == categoryFilter) &&
            (query.isBlank() || app.appName.contains(query, true) || app.shortDescription.contains(query, true) || app.developerName.contains(query, true))
    }.sortedByDescending { it.rewardPerTesterCoins }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            placeholder = { Text("Search apps, developers, keywords") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true, shape = MaterialTheme.shapes.medium
        )
        ScrollableTabRow(selectedTabIndex = categories.indexOf(categoryFilter).coerceAtLeast(0), edgePadding = 16.dp) {
            categories.forEach { cat ->
                Tab(selected = categoryFilter == cat, onClick = { categoryFilter = cat }, text = { Text(cat) })
            }
        }
        if (results.isEmpty()) {
            EmptyState(Icons.Filled.SearchOff, "No matching opportunities", "Try a different search term or category. Remember: you cannot join an app you've already tested.")
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
                item { SectionHeader("${results.size} Open Opportunities", "Recruiting up to 12 testers each") }
                items(results) { app ->
                    val alreadyAssigned = myAssignmentAppIds.contains(app.appId)
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        AppCard(app, onClick = { onOpenApp(app.appId) }, trailing = {
                            if (alreadyAssigned) {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall) {
                                    Text("Assigned", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraSmall) {
                                    Text("+${app.rewardPerTesterCoins}", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        })
                    }
                }
            }
        }
    }
}
