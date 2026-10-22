package com.j01000011.nyumba.presentation.tenant

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.rooms.RoomViewModel
import com.j01000011.nyumba.ui.components.*

@Composable
fun MyRoomScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    roomViewModel: RoomViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val room by roomViewModel.selectedRoom.collectAsState()
    val house by roomViewModel.selectedHouse.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(user) {
        user?.currentRoomId?.let { roomViewModel.loadRoomById(it) }
    }

    Scaffold(topBar = { NyumbaTopBar(title = "My room", onBack = onNavigateBack) }) { padding ->
        val u = user
        val r = room
        when {
            u != null && u.currentRoomId == null -> EmptyState(
                icon = Icons.Default.MeetingRoom,
                title = "No room yet",
                message = "Once a landlord approves your request, your room details will appear here.",
                modifier = Modifier.padding(padding)
            )
            r == null -> LoadingState(Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeroCard {
                    Text("Your room", style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.8f))
                    Text(r.roomName, style = MaterialTheme.typography.headlineMedium)
                    house?.let { Text(it.name, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.85f)) }
                }

                SurfaceCard {
                    Text("Tenancy", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    InfoRow("Monthly rent", formatKwacha(u?.monthlyRent?.takeIf { it > 0 } ?: r.monthlyRent))
                    u?.let { InfoRow("Rent due", "Day ${it.paymentDueDay} of each month") }
                    InfoRow("Room shared by", "${r.occupiedCount} of ${r.capacity} tenant(s)")
                    if (r.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(r.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                house?.let { h ->
                    SurfaceCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Default.Apartment, tone = Tone.Brand)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(h.name, style = MaterialTheme.typography.titleMedium)
                                if (h.address.isNotBlank()) {
                                    Text(h.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (h.phone.isNotBlank()) {
                                FilledTonalIconButton(onClick = {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${h.phone}")))
                                }) { Icon(Icons.Default.Call, contentDescription = "Call ${h.name}") }
                            }
                        }
                        if (h.houseRules.isNotBlank()) {
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            Text("House rules", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(h.houseRules, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
