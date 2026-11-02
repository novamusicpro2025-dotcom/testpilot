package com.nova.a12tester.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.data.*
import com.nova.a12tester.domain.*
import com.nova.a12tester.ui.components.TeamHeroCard

/**
 * Dedicated Teams screen displaying all reciprocal closed testing cohort teams
 * (Team 1, Team 2, Team 3, etc.).
 */
@Composable
fun TeamsScreen(
    onOpenSubmit: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAssignment: (String) -> Unit,
    onOpenApp: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by TestPilotRepository.state.collectAsState()
    val user = state.currentUser ?: return

    // Ensure all teams are loaded, and ensure if any team reaches 13 members, the next team is auto-created
    val teams = remember(state.cohortTeams) {
        val verified = TestPilotRepository.ensureRecruitingTeamAvailable(state.cohortTeams)
        // User should only see the team they have joined. If they haven't joined any, show available teams.
        val joinedTeams = verified.filter { t -> t.members.any { it.userId == user.userId } }
        if (joinedTeams.isNotEmpty()) {
            joinedTeams.sortedBy { it.teamNumber }
        } else {
            verified.sortedBy { it.teamNumber }
        }
    }

    val mySubmittedApps = state.apps.filter { it.developerId == user.userId }
    val myTeam = teams.firstOrNull { t -> t.members.any { it.userId == user.userId } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    "Reciprocal Testing Cohorts (${teams.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (myTeam != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF10B981).copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                    Text(
                                        "Joined ${myTeam.teamName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        "Testing Cohort Teams",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        "Join a 13-developer cohort team. Each developer tests the other 12 apps daily for 2 minutes for 14 days so every app gets guaranteed Google Play testers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Button to create another team
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { TestPilotRepository.createNewCohortTeam() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Team ${teams.size + 1}")
                        }


                    }
                }
            }
        }

        // Teams List: Team 1, Team 2, Team 3, etc.
        items(teams, key = { it.teamId }) { team ->
            Box(Modifier.padding(horizontal = 20.dp)) {
                TeamHeroCard(
                    team = team,
                    currentUser = user,
                    mySubmittedApps = mySubmittedApps,
                    allAssignments = state.assignments,
                    usageSessions = state.usageSessions,
                    onOpenSubmit = onOpenSubmit,
                    onOpenWallet = onOpenWallet,
                    onJoinTeam = { appId -> TestPilotRepository.joinCohortTeam(appId, team.teamId) },
                    onRecordUsage = { aId, sec -> TestPilotRepository.recordAppUsage(aId, sec) },
                    onOpenAssignment = onOpenAssignment,
                    onOpenApp = onOpenApp,
                    onWatchAd = {
                        if (activity != null) {
                            AdMobManager.showRewardedAd(
                                activity = activity,
                                onUserEarnedReward = {
                                    TestPilotRepository.creditRewardedAdCompletion(java.util.UUID.randomUUID().toString())
                                },
                                onAdFailed = {
                                    onOpenWallet()
                                }
                            )
                        } else {
                            onOpenWallet()
                        }
                    }
                )
            }
        }
    }
}
