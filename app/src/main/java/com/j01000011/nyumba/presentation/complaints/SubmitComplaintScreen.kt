package com.j01000011.nyumba.presentation.complaints

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.ComplaintCategory
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubmitComplaintScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    complaintViewModel: ComplaintViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val uiState by complaintViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ComplaintCategory.OTHER) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var submitted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }
    val preview = remember(selectedImageUri) {
        selectedImageUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ComplaintUiState.Success) {
            complaintViewModel.clearState()
            onNavigateBack()
        }
    }

    val submit = {
        submitted = true
        val u = user
        if (u != null && title.isNotBlank() && description.isNotBlank()) {
            complaintViewModel.submitComplaint(
                tenantId = u.uid,
                landlordId = u.landlordId ?: "",
                boardingHouseId = u.boardingHouseId ?: "",
                title = title.trim(),
                description = description.trim(),
                category = selectedCategory,
                imageUri = selectedImageUri
            )
        }
    }

    Scaffold(topBar = { NyumbaTopBar(title = "Report an issue", onBack = onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("What is it about?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ComplaintCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(humanize(category.name)) },
                        leadingIcon = { Icon(category.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            NyumbaTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                label = "Subject",
                leadingIcon = Icons.Default.Title,
                placeholder = "e.g. Bathroom tap is leaking",
                isError = submitted && title.isBlank(),
                supportingText = if (submitted && title.isBlank()) "Add a short subject" else null
            )
            NyumbaTextField(
                value = description,
                onValueChange = { description = it },
                label = "Describe the problem",
                singleLine = false,
                minLines = 4,
                isError = submitted && description.isBlank(),
                supportingText = if (submitted && description.isBlank()) "Tell your landlord what is wrong" else "Where it is, when it started, how urgent it is"
            )

            SectionHeader("Photo (optional)")
            if (preview != null) {
                Box {
                    Image(
                        bitmap = preview,
                        contentDescription = "Selected photo",
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop
                    )
                    FilledTonalIconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) { Icon(Icons.Default.Close, contentDescription = "Remove photo") }
                }
            } else {
                OutlinedButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add a photo")
                }
            }

            (uiState as? ComplaintUiState.Error)?.let {
                InlineMessage(text = it.message, icon = Icons.Default.ErrorOutline)
            }

            PrimaryButton(
                text = "Submit complaint",
                onClick = submit,
                loading = uiState is ComplaintUiState.Loading,
                icon = Icons.AutoMirrored.Filled.Send
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
