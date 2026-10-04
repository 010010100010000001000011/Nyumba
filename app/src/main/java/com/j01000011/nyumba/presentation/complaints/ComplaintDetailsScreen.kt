package com.j01000011.nyumba.presentation.complaints

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.ComplaintStatus
import com.j01000011.nyumba.domain.model.UserRole
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailsScreen(
    complaintId: String,
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    complaintViewModel: ComplaintViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val complaint by complaintViewModel.selectedComplaint.collectAsState()
    val uiState by complaintViewModel.uiState.collectAsState()

    var resolutionComment by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(false) }

    LaunchedEffect(complaintId) {
        complaintViewModel.loadComplaintById(complaintId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complaint Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState is ComplaintUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                complaint?.let { c ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(c.category.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    ComplaintStatusChip(c.status)
                                }
                                Text(c.title, style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    text = "Submitted: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(c.createdAt))}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(c.description, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        if (!c.localImagePath.isNullOrBlank()) {
                            val imgFile = File(c.localImagePath)
                            if (imgFile.exists()) {
                                Text("Attached Image:", style = MaterialTheme.typography.titleMedium)
                                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Complaint Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(250.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        if (c.status == ComplaintStatus.RESOLVED) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Resolution", style = MaterialTheme.typography.titleMedium)
                                    Text(c.landlordComment ?: "No comment provided.", style = MaterialTheme.typography.bodyMedium)
                                    c.resolvedAt?.let {
                                        Text(
                                            text = "Resolved on: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        } else if (user?.role == UserRole.LANDLORD) {
                            if (isResolving) {
                                OutlinedTextField(
                                    value = resolutionComment,
                                    onValueChange = { resolutionComment = it },
                                    label = { Text("Resolution Comment") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { isResolving = false }) { Text("Cancel") }
                                    Button(onClick = {
                                        complaintViewModel.resolveComplaint(c.complaintId, resolutionComment)
                                        isResolving = false
                                    }) {
                                        Text("Mark Resolved")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { isResolving = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Resolve Complaint")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
