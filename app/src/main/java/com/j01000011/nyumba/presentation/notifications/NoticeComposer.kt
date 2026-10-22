package com.j01000011.nyumba.presentation.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.j01000011.nyumba.ui.components.*

private val templates = listOf(
    "Rent reminder" to "Friendly reminder that rent is due soon. Please pay on time via the Nyumba app.",
    "Water outage" to "Water supply will be interrupted. We apologise for the inconvenience and will update you once restored.",
    "Power outage" to "There is a scheduled power interruption. Please plan accordingly.",
    "Maintenance" to "Maintenance work is scheduled at the house. Please allow access where needed.",
    "House meeting" to "There will be a short house meeting. Your attendance is appreciated."
)

/** Subject/body composer with quick templates, shared by direct notices and broadcasts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NoticeComposer(
    recipientHeader: @Composable () -> Unit,
    sendLabel: String,
    sending: Boolean,
    canSend: Boolean,
    modifier: Modifier = Modifier,
    onSend: (title: String, message: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        recipientHeader()
        SectionHeader("Quick templates")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            templates.forEach { (t, m) ->
                SuggestionChip(onClick = { title = t; message = m }, label = { Text(t) })
            }
        }
        NyumbaTextField(value = title, onValueChange = { if (it.length <= 80) title = it }, label = "Subject", leadingIcon = Icons.Default.Title)
        NyumbaTextField(
            value = message,
            onValueChange = { if (it.length <= 1000) message = it },
            label = "Message",
            singleLine = false,
            minLines = 5,
            supportingText = "${message.length}/1000"
        )
        PrimaryButton(
            text = sendLabel,
            onClick = { onSend(title.trim(), message.trim()) },
            enabled = canSend && title.isNotBlank() && message.isNotBlank(),
            loading = sending,
            icon = Icons.AutoMirrored.Filled.Send
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun RecipientCard(title: String, subtitle: String, icon: @Composable () -> Unit) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
