package com.j01000011.nyumba.presentation.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val user by authViewModel.currentUser.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }

    Scaffold(
        topBar = { NyumbaTopBar(title = "Settings", onBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Preferences")
            SurfaceCard {
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Rent reminders, payments and messages",
                    trailing = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
                        }
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    title = "Appearance",
                    subtitle = "Follows your phone's light or dark mode"
                )
            }

            SectionHeader("Security")
            SurfaceCard {
                SettingsRow(
                    icon = Icons.Default.LockReset,
                    title = "Change password",
                    subtitle = user?.email?.let { "We'll email a reset link to $it" } ?: "Email a password reset link",
                    onClick = { if (user?.email?.isNotBlank() == true) confirmReset = true }
                )
            }

            SectionHeader("About")
            SurfaceCard {
                SettingsRow(icon = Icons.Default.Info, title = "Nyumba", subtitle = "Version $versionName")
            }
        }

        if (confirmReset) {
            AlertDialog(
                onDismissRequest = { confirmReset = false },
                icon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                title = { Text("Reset password?") },
                text = { Text("We'll send a link to ${user?.email} so you can choose a new password.") },
                confirmButton = {
                    Button(onClick = {
                        confirmReset = false
                        val email = user?.email ?: return@Button
                        authViewModel.sendPasswordReset(email) { error ->
                            scope.launch { snackbar.showSnackbar(error ?: "Reset link sent to $email") }
                        }
                    }) { Text("Send link") }
                },
                dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, tone = Tone.Brand, size = 40)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}
