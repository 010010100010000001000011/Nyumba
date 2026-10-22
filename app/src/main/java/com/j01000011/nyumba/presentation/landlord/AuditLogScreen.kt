package com.j01000011.nyumba.presentation.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.AuditLog
import com.j01000011.nyumba.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuditLogScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: AuditLogViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadLogs(houseId)
    }

    val dayFormat = remember { SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()) }
    val grouped = logs.groupBy { dayFormat.format(Date(it.timestamp)) }

    Scaffold(topBar = { NyumbaTopBar(title = "Audit log", subtitle = "Every change in this house", onBack = onNavigateBack) }) { padding ->
        if (logs.isEmpty()) {
            EmptyState(
                icon = Icons.Default.History,
                title = "No activity yet",
                message = "Room changes, payments, approvals and other actions are recorded here.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (day, dayLogs) ->
                    item(key = "h-$day") { SectionHeader(day) }
                    items(dayLogs, key = { it.logId }) { log -> AuditLogItem(log) }
                }
            }
        }
    }
}

@Composable
fun AuditLogItem(log: AuditLog) {
    val (icon, tone) = auditStyle(log.action)
    ListItem(
        headlineContent = { Text(humanize(log.action), style = MaterialTheme.typography.titleSmall) },
        supportingContent = {
            val details = log.metadata.entries.joinToString(" · ") { (k, v) -> "${humanize(k)}: $v" }
            Text(
                listOf(humanize(log.entityType), details).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        },
        leadingContent = { IconBadge(icon, tone = tone, size = 40) },
        trailingContent = {
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.padding(vertical = 0.dp)
    )
}

private fun auditStyle(action: String): Pair<ImageVector, Tone> = when {
    action.startsWith("PAYMENT") -> Icons.Default.Payments to Tone.Success
    action.startsWith("ROOM_REQUEST") -> Icons.AutoMirrored.Filled.Assignment to Tone.Info
    action.startsWith("ROOM") -> Icons.Default.MeetingRoom to Tone.Brand
    action.startsWith("COMPLAINT") -> Icons.Default.ReportProblem to Tone.Warning
    action.startsWith("TENANT") -> Icons.Default.Person to Tone.Danger
    action.startsWith("CONVERSATION") || action.startsWith("MESSAGE") -> Icons.AutoMirrored.Filled.Chat to Tone.Info
    else -> Icons.Default.History to Tone.Neutral
}
