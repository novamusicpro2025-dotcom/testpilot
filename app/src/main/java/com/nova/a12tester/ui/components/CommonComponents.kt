package com.nova.a12tester.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nova.a12tester.domain.ModerationStatus
import com.nova.a12tester.domain.SubmittedApp
import com.nova.a12tester.domain.VerificationType
import com.nova.a12tester.ui.theme.isAppInDarkTheme

/** Section header used across dashboards. */
@Composable
fun SectionHeader(title: String, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) trailing()
    }
}

/** Statistic card with colorful icon badge. */
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    container: Color = MaterialTheme.colorScheme.surface,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = content)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Verification badge distinguishing user-reported vs server-verified evidence (Play compliance). */
@Composable
fun VerificationBadge(type: VerificationType, modifier: Modifier = Modifier) {
    val (icon, tint, label) = when (type) {
        VerificationType.USER_REPORTED -> Triple(Icons.Filled.Visibility, MaterialTheme.colorScheme.secondary, "User-Reported")
        VerificationType.SERVER_TIMESTAMPED_CHECKIN -> Triple(Icons.Filled.VerifiedUser, MaterialTheme.colorScheme.tertiary, "Server-Timestamped")
        VerificationType.FEEDBACK_ARTIFACT_VERIFIED -> Triple(Icons.Filled.VerifiedUser, MaterialTheme.colorScheme.primary, "Verified Artifact")
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = tint.copy(alpha = 0.14f)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Compliance/disclaimer callout. */
@Composable
fun ComplianceNote(text: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

/** Gradient banner used for coin balance and wallet hero. */
@Composable
fun GradientBanner(
    modifier: Modifier = Modifier,
    topColor: Color,
    bottomColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier, shape = MaterialTheme.shapes.large) {
        Column(Modifier.background(Brush.linearGradient(listOf(topColor, bottomColor))).padding(20.dp), content = content)
    }
}

/** Empty state with icon, title, message and optional action. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) action()
    }
}

/** Error banner for inline, recoverable errors. */
@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

/** Full-screen loading state. */
@Composable
fun LoadingView(modifier: Modifier = Modifier.fillMaxSize(), message: String = "Loading…") {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Skeleton placeholder card for loading lists. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(modifier.height(84.dp), shape = MaterialTheme.shapes.medium) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))
    }
}

/** Shimmer-style loading placeholder list. */
@Composable
fun LoadingList(modifier: Modifier = Modifier, items: Int = 4) {
    Column(modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(items) {
            Card(Modifier.fillMaxWidth().height(84.dp), shape = MaterialTheme.shapes.medium) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).animateContentSize())
            }
        }
    }
}

/** App list card showing icon placeholder, name, developer, reward and moderation state. */
@Composable
fun AppCard(
    app: SubmittedApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    trailing: @Composable (() -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    val isVip = app.isVipTopPinned
    val isPremium = app.isFeaturedPremium
    val cardColors = when {
        isVip -> CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1435) else Color(0xFFFAF5FF),
            contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E1B4B)
        )
        isPremium -> CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1F1A12) else Color(0xFFFFFBEB),
            contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
        )
        else -> CardDefaults.cardColors()
    }
    val cardBorder = when {
        isVip -> BorderStroke(1.8.dp, if (isDark) Color(0xFFA78BFA) else Color(0xFF8B5CF6))
        isPremium -> BorderStroke(1.5.dp, Color(0xFFF59E0B))
        else -> null
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = cardColors,
        border = cardBorder
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isVip) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE), shape = MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("👑", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "#1 TOP SPOTLIGHT • EARN +70 COINS • 🛡️ PLAY STORE GUARANTEED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
                    )
                }
            } else if (isPremium) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(if (isDark) Color(0xFF382307) else Color(0xFFFEF3C7), shape = MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("⭐", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "PRO FEATURED • EARN +${app.rewardPerTesterCoins} COINS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when {
                        isVip -> Color(0xFF7C3AED)
                        isPremium -> Color(0xFFF59E0B)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            app.appName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isVip || isPremium) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(app.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("${app.developerName} • ${app.testingDurationDays}-day test", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (trailing != null) trailing()
            }
            Text(app.shortDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModerationStatusChip(app.moderationStatus)
                Text(
                    "Reward +${app.rewardPerTesterCoins} coins • ${app.assignedTesterCount}/${app.targetTesterCount} testers",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isVip || isPremium) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isVip -> if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9)
                        isPremium -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}
