package com.nova.a12tester.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.ui.components.SectionHeader

/**
 * Features & Platform Guide Screen.
 * Showcases all unique capabilities of the 12tester platform.
 */
@Composable
fun FeaturesScreen(
    onOpenSubmit: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenTesting: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
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
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.offset(x = (-10).dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("12tester Platform Guide", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        "Closed Testing Made Easy & Guaranteed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        "Designed specifically for Google Play's 2026 14-day closed testing requirements with real human testers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Feature 1: Reciprocal Cohort Teams
        item {
            FeatureDetailCard(
                icon = Icons.Filled.Groups,
                badge = "Core Architecture",
                title = "13-Developer Reciprocal Cohorts",
                description = "Each team holds exactly 13 developers. When slot #13 joins, the team automatically locks and activates for 14 days, and the next team (Team 2, Team 3...) is created automatically.",
                bulletPoints = listOf(
                    "You test the remaining 12 peer apps daily for 2 minutes",
                    "The other 12 developers install and test your app daily",
                    "Guaranteed reciprocal 12 testers for 14 consecutive days"
                ),
                accentColor = Color(0xFF6366F1),
                bgColor = Color(0xFFEEF2FF),
                actionLabel = "View Testing Tasks",
                onAction = onOpenTesting
            )
        }

        item { Spacer(Modifier.height(14.dp)) }

        // Feature 2: Real OS Usage Verification
        item {
            FeatureDetailCard(
                icon = Icons.Filled.Timer,
                badge = "Anti-Cheat Protection",
                title = "2-Minute Android OS Usage Tracking",
                description = "We integrate directly with Android's UsageStatsManager to verify actual foreground time spent in the test apps.",
                bulletPoints = listOf(
                    "Requires 120 seconds of real active screen time daily",
                    "Anti-cheat verification prevents fake clicks or instant check-ins",
                    "Automatic check-in recorded immediately when 2 minutes is reached"
                ),
                accentColor = Color(0xFF10B981),
                bgColor = Color(0xFFECFDF5)
            )
        }

        item { Spacer(Modifier.height(14.dp)) }

        // Feature 3: Daily Quota Enforcement
        item {
            FeatureDetailCard(
                icon = Icons.Filled.Shield,
                badge = "Fairness Guarantee",
                title = "Mandatory 10-App Daily Quota",
                description = "To ensure reciprocal fairness, active developers who submit an app must test a minimum of 10 apps daily for 2 minutes each.",
                bulletPoints = listOf(
                    "Failing daily quota puts your app in temporary suspension",
                    "Testing 10 apps today immediately reactivates your app",
                    "Protects honest developers from freeloaders who never test back"
                ),
                accentColor = Color(0xFFF59E0B),
                bgColor = Color(0xFFFFFBEB)
            )
        }

        item { Spacer(Modifier.height(14.dp)) }

        // Feature 4: Coin Economy & Free Ads
        item {
            FeatureDetailCard(
                icon = Icons.Filled.MonetizationOn,
                badge = "100% Free Economy",
                title = "Coin Rewards & AdMob Support",
                description = "No real money needed. Earn coins by testing community apps, or watch short rewarded video ads to earn submission coins instantly.",
                bulletPoints = listOf(
                    "Earn +25 coins for completing 14-day testing cycles",
                    "Watch quick AdMob video ads for +10 coins anytime",
                    "Submit new apps for 300 coins or join teams"
                ),
                accentColor = Color(0xFF8B5CF6),
                bgColor = Color(0xFFF5F3FF),
                actionLabel = "Open Coin Wallet",
                onAction = onOpenWallet
            )
        }

        item { Spacer(Modifier.height(20.dp)) }

        // Call to action button at bottom
        item {
            Box(Modifier.padding(horizontal = 20.dp)) {
                Button(
                    onClick = onOpenSubmit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.AddCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Submit Your App For Testing", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FeatureDetailCard(
    icon: ImageVector,
    badge: String,
    title: String,
    description: String,
    bulletPoints: List<String>,
    accentColor: Color,
    bgColor: Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = bgColor,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bulletPoints.forEach { point ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Text(point, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
