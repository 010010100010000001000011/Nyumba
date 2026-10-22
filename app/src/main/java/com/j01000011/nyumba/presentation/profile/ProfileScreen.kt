package com.j01000011.nyumba.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.UserRole
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.landlord.InitialsAvatar
import com.j01000011.nyumba.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }

    Scaffold(topBar = { NyumbaTopBar(title = "Profile", onBack = onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                InitialsAvatar(user?.fullName ?: "", size = 88)
                Spacer(Modifier.height(12.dp))
                Text(user?.fullName ?: "", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                user?.role?.let { role ->
                    StatusPill(if (role == UserRole.LANDLORD) "Landlord" else "Tenant", if (role == UserRole.LANDLORD) Tone.Info else Tone.Brand)
                }
            }

            SurfaceCard {
                Text("Account", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                InfoRow("Email", user?.email?.ifBlank { null } ?: "—")
                InfoRow("Phone", user?.phoneNumber?.ifBlank { null } ?: "—")
                if (user?.role == UserRole.TENANT) {
                    InfoRow("NRC / ID", user?.identificationNumber?.ifBlank { null } ?: "—")
                    InfoRow(
                        "Emergency contact",
                        listOfNotNull(user?.emergencyContactName?.ifBlank { null }, user?.emergencyContactPhone?.ifBlank { null })
                            .joinToString(" · ").ifBlank { "—" }
                    )
                }
                user?.let { InfoRow("Member since", SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(it.createdAt))) }
            }

            SurfaceCard {
                NavRow(Icons.Default.Settings, "Settings", "Notifications, password, about", onNavigateToSettings)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                NavRow(Icons.Default.Collections, "Saved photos", "Photos attached to your complaints", onNavigateToGallery)
            }

            OutlinedButton(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign out")
            }
        }

        if (confirmLogout) {
            AlertDialog(
                onDismissRequest = { confirmLogout = false },
                title = { Text("Sign out?") },
                text = { Text("You'll need your email and password to sign back in.") },
                confirmButton = {
                    Button(onClick = {
                        confirmLogout = false
                        viewModel.logout()
                        onLogout()
                    }) { Text("Sign out") }
                },
                dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, tone = Tone.Brand, size = 40)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
