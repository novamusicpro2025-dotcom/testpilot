package com.nova.a12tester.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.a12tester.domain.*

/** Status chip for an app's moderation state (single source of truth). */
@Composable
fun ModerationStatusChip(status: ModerationStatus, modifier: Modifier = Modifier) {
    val (container, content) = when (status) {
        ModerationStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ModerationStatus.PENDING_REVIEW -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ModerationStatus.REJECTED, ModerationStatus.SUSPENDED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        ModerationStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.extraSmall, color = container, modifier = modifier) {
        Text(status.label, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = content, fontWeight = FontWeight.Medium)
    }
}

/** Status chip for a tester's assignment state (single source of truth). */
@Composable
fun AssignmentStatusChip(status: AssignmentStatus, modifier: Modifier = Modifier) {
    val (container, content) = when (status) {
        AssignmentStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        AssignmentStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        AssignmentStatus.ASSIGNED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        AssignmentStatus.DISPUTED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.extraSmall, color = container, modifier = modifier) {
        Text(status.label, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = content, fontWeight = FontWeight.Medium)
    }
}

/** Simple read-only chip used for labels like "Assigned". */
@Composable
fun LabelChip(text: String, container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant, content: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.extraSmall, color = container, modifier = modifier) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = content)
    }
}

/** Status chip for user account status (ACTIVE, SUSPENDED, DELETED). */
@Composable
fun AccountStatusChip(status: AccountStatus, modifier: Modifier = Modifier) {
    val (container, content) = when (status) {
        AccountStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        AccountStatus.SUSPENDED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        AccountStatus.DELETED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.extraSmall, color = container, modifier = modifier) {
        Text(status.label, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = content, fontWeight = FontWeight.Medium)
    }
}
