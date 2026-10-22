package com.j01000011.nyumba.presentation.gallery

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.j01000011.nyumba.core.util.ImageStorageService
import com.j01000011.nyumba.ui.components.EmptyState
import com.j01000011.nyumba.ui.components.NyumbaTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit,
    imageStorageService: ImageStorageService
) {
    var images by remember { mutableStateOf(imageStorageService.getAllImages().sortedByDescending { it.lastModified() }) }
    var selectedImage by remember { mutableStateOf<File?>(null) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }

    Scaffold(
        topBar = {
            NyumbaTopBar(
                title = "Saved photos",
                subtitle = if (images.isEmpty()) null else "${images.size} on this device",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        if (images.isEmpty()) {
            EmptyState(
                icon = Icons.Default.PhotoLibrary,
                title = "No photos yet",
                message = "Photos you attach to complaints are stored here on your device.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images, key = { it.absolutePath }) { file ->
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { selectedImage = file }
                    ) {
                        Thumbnail(file, maxSize = 320, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        FilledTonalIconButton(
                            onClick = { pendingDelete = file },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp)
                        ) { Icon(Icons.Default.Delete, contentDescription = "Delete photo", modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }

        selectedImage?.let { file ->
            Dialog(onDismissRequest = { selectedImage = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    Thumbnail(file, maxSize = 2048, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    IconButton(
                        onClick = { selectedImage = null },
                        modifier = Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(8.dp)
                    ) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                    Text(
                        "Saved ${SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(file.lastModified()))}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(16.dp)
                    )
                }
            }
        }

        pendingDelete?.let { file ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Delete photo?") },
                text = { Text("It is removed from this device permanently.") },
                confirmButton = {
                    Button(
                        onClick = {
                            imageStorageService.deleteImage(file.absolutePath)
                            images = imageStorageService.getAllImages().sortedByDescending { it.lastModified() }
                            pendingDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
            )
        }
    }
}

/** Decodes [file] off the main thread, downsampled so the longest side is about [maxSize] px. */
@Composable
private fun Thumbnail(file: File, maxSize: Int, modifier: Modifier, contentScale: ContentScale) {
    val bitmap by produceState<ImageBitmap?>(null, file, maxSize) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSize) sample *= 2
                BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        }
    }
    bitmap?.let { Image(bitmap = it, contentDescription = null, modifier = modifier, contentScale = contentScale) }
}
