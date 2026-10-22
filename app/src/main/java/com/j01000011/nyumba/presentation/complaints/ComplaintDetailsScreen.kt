package com.j01000011.nyumba.presentation.complaints

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.ComplaintStatus
import com.j01000011.nyumba.domain.model.UserRole
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.components.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ComplaintDetailsScreen(
    complaintId: String,
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    complaintViewModel: ComplaintViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val complaint by complaintViewModel.selectedComplaint.collectAsState()

    var resolutionComment by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(false) }

    LaunchedEffect(complaintId) {
        complaintViewModel.loadComplaintById(complaintId)
    }

    val dateTime = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }

    Scaffold(topBar = { NyumbaTopBar(title = "Complaint", onBack = onNavigateBack) }) { padding ->
        val c = complaint
        if (c == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(c.category.icon(), tone = c.status.tone())
                    Spacer(Modifier.width(12.dp))
                    Text(
                        humanize(c.category.name),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    ComplaintStatusChip(c.status)
                }
                Spacer(Modifier.height(14.dp))
                Text(c.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(c.description, style = MaterialTheme.typography.bodyLarge)
            }

            SurfaceCard {
                Text("Timeline", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                InfoRow("Submitted", dateTime.format(Date(c.createdAt)))
                c.lastReminderAt?.let { InfoRow("Last reminder", dateTime.format(Date(it))) }
                c.resolvedAt?.let { InfoRow("Resolved", dateTime.format(Date(it))) }
            }

            c.localImagePath?.takeIf { it.isNotBlank() }?.let { path ->
                val bitmap = remember(path) {
                    File(path).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
                }
                if (bitmap != null) {
                    SectionHeader("Photo")
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Complaint photo",
                        modifier = Modifier.fillMaxWidth().height(240.dp).clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            if (c.status == ComplaintStatus.RESOLVED) {
                Surface(color = Tone.Success.containerColor(), shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tone.Success.contentColor())
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Resolved", style = MaterialTheme.typography.titleMedium, color = Tone.Success.contentColor())
                            Text(
                                c.landlordComment?.ifBlank { null } ?: "No comment from the landlord.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Tone.Success.contentColor()
                            )
                        }
                    }
                }
            } else if (user?.role == UserRole.LANDLORD) {
                if (isResolving) {
                    SurfaceCard {
                        Text("Resolve this complaint", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        NyumbaTextField(
                            value = resolutionComment,
                            onValueChange = { resolutionComment = it },
                            label = "What was done? (shared with tenant)",
                            singleLine = false,
                            minLines = 3
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { isResolving = false },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) { Text("Cancel") }
                            Button(
                                onClick = {
                                    complaintViewModel.resolveComplaint(c.complaintId, resolutionComment.ifBlank { null })
                                    isResolving = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) { Text("Mark resolved") }
                        }
                    }
                } else {
                    PrimaryButton(text = "Resolve complaint", onClick = { isResolving = true }, icon = Icons.Default.TaskAlt)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
