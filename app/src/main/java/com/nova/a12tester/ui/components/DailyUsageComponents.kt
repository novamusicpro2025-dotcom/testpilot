package com.nova.a12tester.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nova.a12tester.data.UsageStatsHelper
import com.nova.a12tester.domain.DailyAppUsageSession
import com.nova.a12tester.domain.DailyTesterQuotaStatus
import kotlinx.coroutines.delay

/**
 * Prominent Dashboard Card showing the mandatory 10-app daily testing quota status.
 * Explains that each tester must test at least 10 apps for 2 minutes daily,
 * otherwise their own submitted apps will be removed from testing.
 */
@Composable
fun DailyQuotaCard(
    quota: DailyTesterQuotaStatus,
    onFindMoreApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (quota.isQuotaMet) Icons.Filled.Verified else Icons.Filled.Timer,
                        contentDescription = null,
                        tint = if (quota.isQuotaMet) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                    Text(
                        "Daily Testing Quota",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (quota.isQuotaMet) Color(0xFF10B981) else Color(0xFFF59E0B)
                ) {
                    Text(
                        "${quota.completedAppsCount} / ${quota.requiredAppsCount} Apps",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LinearProgressIndicator(
                progress = { quota.progressFraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (quota.isQuotaMet) Color(0xFF10B981) else Color(0xFFF59E0B),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                "Mandatory: Open and use at least 10 test apps for 2+ minutes daily.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Warning callout regarding app removal
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (quota.isQuotaMet) Color(0xFF10B981).copy(alpha = 0.12f)
                else Color(0xFFF59E0B).copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (quota.isQuotaMet) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (quota.isQuotaMet) Color(0xFF059669) else Color(0xFFD97706)
                    )
                    Text(
                        if (quota.isQuotaMet) {
                            "Quota complete for today! Your submitted apps are active and protected."
                        } else {
                            "⚠️ If you don't test 10 apps daily (2 mins each), your submitted app will be REMOVED from testing!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (quota.isQuotaMet) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Alert if any apps were already removed
            if (quota.removedAppsDueToQuota.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            "🚨 ${quota.removedAppsDueToQuota.size} App(s) Removed from Testing",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Your app was removed because yesterday's quota was not met. Complete 10 tests today to reinstate it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!quota.isQuotaMet) {
                    TextButton(onClick = onFindMoreApps) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Find More Apps to Test")
                    }
                }
            }
        }
    }
}

/**
 * Banner shown on Home and Testing screens when Android Usage Access permission has not been granted.
 * Prompts the tester to enable it so foreground usage is tracked automatically.
 */
@Composable
fun UsageAccessBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(UsageStatsHelper.isUsageAccessPermissionGranted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = UsageStatsHelper.isUsageAccessPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!hasPermission) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.SecurityUpdateWarning,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(30.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Usage Access Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                    Text(
                        "Grant 'Usage Access' permission in Android Settings so 12tester can automatically record your 2-minute daily testing without manual timers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB45309)
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { UsageStatsHelper.openUsageAccessSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Enable in Settings", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * 2-minute live test card for an individual assigned app.
 * Provides live countdown timer, launch trigger, simulated 2-min usage, and OS usage sync.
 */
@Composable
fun AppTestUsageCard(
    appName: String,
    session: DailyAppUsageSession?,
    onAddSeconds: (Int) -> Unit,
    onLaunchApp: () -> Unit,
    onSyncNow: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(UsageStatsHelper.isUsageAccessPermissionGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = UsageStatsHelper.isUsageAccessPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val duration = session?.durationSeconds ?: 0
    val targetMet = session?.isTargetMet == true
    var isTimerActive by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerActive) {
        if (isTimerActive) {
            while (isTimerActive) {
                delay(1000)
                onAddSeconds(1)
                if ((session?.durationSeconds ?: 0) + 1 >= 120) {
                    isTimerActive = false
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (targetMet) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (targetMet) Icons.Filled.CheckCircle else Icons.Filled.Timelapse,
                        contentDescription = null,
                        tint = if (targetMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                    Text("Daily 2-Min Usage Test", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (targetMet) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "${session?.formattedDuration ?: "00:00"} / 02:00",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (targetMet) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { session?.progressFraction ?: 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (targetMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )

            Text(
                if (targetMet) "✅ 2-minute daily testing requirement completed for today!"
                else "You have tested for ${duration}s. Need ${(120 - duration).coerceAtLeast(0)}s more to fulfill today's requirement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasPermission) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "⚠️ Enable 'Usage Access' for auto-tracking",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF92400E),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { UsageStatsHelper.openUsageAccessSettings(context) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Grant", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (!targetMet) {
                    Button(
                        onClick = {
                            if (!hasPermission) {
                                UsageStatsHelper.openUsageAccessSettings(context)
                            }
                            onLaunchApp()
                            isTimerActive = !isTimerActive
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (isTimerActive) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isTimerActive) "Pause Timer" else "Launch & Test")
                    }
                } else {
                    OutlinedButton(
                        onClick = onLaunchApp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open $appName")
                    }
                }

                if (onSyncNow != null) {
                    FilledTonalIconButton(onClick = onSyncNow) {
                        Icon(Icons.Filled.Sync, contentDescription = "Sync Usage")
                    }
                }
            }
        }
    }
}

