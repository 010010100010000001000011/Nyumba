package com.j01000011.nyumba.presentation.complaints

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.j01000011.nyumba.domain.model.Complaint
import com.j01000011.nyumba.domain.model.ComplaintCategory
import com.j01000011.nyumba.domain.model.ComplaintStatus
import com.j01000011.nyumba.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

fun ComplaintCategory.icon(): ImageVector = when (this) {
    ComplaintCategory.PLUMBING -> Icons.Default.Plumbing
    ComplaintCategory.ELECTRICITY -> Icons.Default.ElectricBolt
    ComplaintCategory.WATER -> Icons.Default.WaterDrop
    ComplaintCategory.MAINTENANCE -> Icons.Default.Build
    ComplaintCategory.SECURITY -> Icons.Default.Shield
    ComplaintCategory.NOISE -> Icons.AutoMirrored.Filled.VolumeUp
    ComplaintCategory.INTERNET -> Icons.Default.Wifi
    ComplaintCategory.OTHER -> Icons.Default.MoreHoriz
}

fun ComplaintStatus.tone(): Tone = when (this) {
    ComplaintStatus.RESOLVED -> Tone.Success
    ComplaintStatus.PENDING -> Tone.Warning
    ComplaintStatus.OVERDUE -> Tone.Danger
}

@Composable
fun ComplaintStatusChip(status: ComplaintStatus) {
    StatusPill(text = humanize(status.name), tone = status.tone())
}

@Composable
fun ComplaintItemCard(complaint: Complaint, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(complaint.createdAt))
    SurfaceCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(complaint.category.icon(), tone = complaint.status.tone(), size = 44)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(complaint.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    complaint.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${humanize(complaint.category.name)} · $dateStr",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    ComplaintStatusChip(complaint.status)
                }
            }
        }
    }
}
