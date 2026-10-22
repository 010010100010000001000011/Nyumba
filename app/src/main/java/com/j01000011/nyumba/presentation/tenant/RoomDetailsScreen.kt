package com.j01000011.nyumba.presentation.tenant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.rooms.RoomUiState
import com.j01000011.nyumba.presentation.rooms.RoomViewModel
import com.j01000011.nyumba.ui.components.*

@Composable
fun RoomDetailsScreen(
    roomId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    roomViewModel: RoomViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val room by roomViewModel.selectedRoom.collectAsState()
    val house by roomViewModel.selectedHouse.collectAsState()
    val roomState by roomViewModel.roomState.collectAsState()

    LaunchedEffect(roomId) {
        roomViewModel.loadRoomById(roomId)
    }
    LaunchedEffect(roomState) {
        if (roomState is RoomUiState.Success) {
            roomViewModel.clearState()
            onNavigateToDashboard()
        }
    }

    Scaffold(
        topBar = { NyumbaTopBar(title = "Room details", subtitle = house?.name, onBack = onNavigateBack) },
        bottomBar = {
            room?.let { r ->
                val free = r.availableCount > 0
                val alreadyHoused = user?.currentRoomId != null
                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                        PrimaryButton(
                            text = when {
                                alreadyHoused -> "You already have a room"
                                free -> "Hold this room for 30 minutes"
                                else -> "Room is full"
                            },
                            onClick = {
                                val u = user ?: return@PrimaryButton
                                roomViewModel.requestRoom(r.roomId, u.uid, u.fullName, r.boardingHouseId)
                            },
                            enabled = free && !alreadyHoused && user != null,
                            loading = roomState is RoomUiState.Loading,
                            icon = Icons.Default.BookmarkAdd
                        )
                    }
                }
            }
        }
    ) { padding ->
        val r = room?.takeIf { it.roomId == roomId }
        if (r == null) {
            (roomState as? RoomUiState.Error)?.let {
                Box(Modifier.padding(padding).padding(20.dp)) { InlineMessage(it.message, Icons.Default.ErrorOutline) }
            } ?: LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroCard {
                Text(r.roomName, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatKwacha(r.monthlyRent)} per month",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalContentColor.current.copy(alpha = 0.9f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("Free beds", "${maxOf(0, r.availableCount)}", Icons.Default.Bed, Modifier.weight(1f), Tone.Success)
                MetricTile("Sharing", "${r.capacity} bed(s)", Icons.Default.People, Modifier.weight(1f), Tone.Info)
                MetricTile("Rent", formatKwacha(r.monthlyRent), Icons.Default.Payments, Modifier.weight(1f))
            }
            if (r.description.isNotBlank()) {
                SurfaceCard {
                    Text("About this room", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(r.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
            house?.houseRules?.takeIf { it.isNotBlank() }?.let { rules ->
                SurfaceCard {
                    Text("House rules", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(rules, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            InlineMessage(
                text = "Holding reserves a bed for 30 minutes while the landlord reviews your request. You'll be notified once they decide.",
                icon = Icons.Default.Info,
                tone = Tone.Info
            )
            (roomState as? RoomUiState.Error)?.let { InlineMessage(it.message, Icons.Default.ErrorOutline) }
        }
    }
}
