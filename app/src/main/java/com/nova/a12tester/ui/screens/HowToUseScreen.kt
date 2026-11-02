package com.nova.a12tester.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.ui.theme.BrandTealDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to Use TestPilot") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Beginner's Guide",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Follow these simple steps to submit your app, join a testing cohort team, and successfully complete Google's 14-day testing requirement.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Step 1: Earning Coins
            GuideSection(
                stepNumber = "1",
                title = "Join a Cohort Team (13 Developers)",
                icon = Icons.Filled.GroupAdd,
                color = Color(0xFF2563EB)
            ) {
                Text("To get testers, you must join a team of 13 developers.")
                BulletPoint("You need 300 coins to submit your app and 200 coins to join a team.")
                BulletPoint("Once you join, you will see 12 other apps from your team members.")
                BulletPoint("You MUST install all 12 peer apps to begin testing.")
            }

            // Step 2: The 14-Day Testing Duty
            GuideSection(
                stepNumber = "2",
                title = "Your Daily Testing Duty",
                icon = Icons.Filled.Timer,
                color = Color(0xFFF59E0B)
            ) {
                Text("Testing is reciprocal. You test their apps, they test yours.")
                BulletPoint("Open each of the 12 apps every day.")
                BulletPoint("Use each app for at least 2 minutes.")
                BulletPoint("Record your daily check-in in the TestPilot app.")
                BulletPoint("You must do this for 14 consecutive days.")
            }

            // Step 3: Strict Penalty System
            GuideSection(
                stepNumber = "3",
                title = "Strict Rules & Auto-Removal",
                icon = Icons.Filled.Warning,
                color = MaterialTheme.colorScheme.error
            ) {
                Text("TestPilot actively tracks daily participation to protect all developers.")
                BulletPoint("If you miss 1 day of testing, you will receive a warning notification.")
                BulletPoint("If you miss 3 days of testing, your testing assignment is ABANDONED.")
                BulletPoint("CRITICAL: If you fail to test the 12 peer apps, YOUR APP will be automatically removed from the team, and a new developer will replace you.")
            }

            // Step 4: Chat & Support
            GuideSection(
                stepNumber = "4",
                title = "Chat with Developers",
                icon = Icons.AutoMirrored.Filled.Chat,
                color = Color(0xFF10B981)
            ) {
                Text("Communication is key to a successful testing phase.")
                BulletPoint("If you face any installation errors, don't just skip the app.")
                BulletPoint("Use the 'Chat with Developer' button on the progress screen.")
                BulletPoint("Explain the issue directly to the developer so they can fix it and you can continue testing.")
            }

            // Step 5: Getting Opt-In Link
            GuideSection(
                stepNumber = "5",
                title = "Get Your Opt-in Link (For your app)",
                icon = Icons.Filled.Link,
                color = BrandTealDark
            ) {
                Text("Before submitting your own app, you need a Google Group and Opt-in link.")
                BulletPoint("Create a Google Group (Set 'Who can join' to 'Anyone').")
                BulletPoint("Add the group email to Play Console > Closed testing > Testers.")
                BulletPoint("Copy the 'Join on the web' link from the Play Console.")
                
                MockScreenshotBox("Play Console - Testers Tab") {
                    Icon(Icons.Filled.ContentCopy, null, tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text("https://play.google.com/apps/testing/...", color = Color.Gray)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun GuideSection(
    stepNumber: String,
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(stepNumber, fontWeight = FontWeight.Bold, color = color)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun MockScreenshotBox(title: String, content: @Composable RowScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }
}
