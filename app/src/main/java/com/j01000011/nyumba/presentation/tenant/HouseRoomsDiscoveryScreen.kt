package com.j01000011.nyumba.presentation.tenant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Room
import com.j01000011.nyumba.domain.model.RoomStatus
import com.j01000011.nyumba.presentation.rooms.RoomViewModel
import com.j01000011.nyumba.ui.components.*

@Composable
fun HouseRoomsDiscoveryScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onRoomSelected: (String) -> Unit,
    roomViewModel: RoomViewModel = hiltViewModel()
) {
    val rooms by roomViewModel.rooms.collectAsState()
    val house by roomViewModel.selectedHouse.collectAsState()

    LaunchedEffect(houseId) {
        roomViewModel.loadRooms(houseId)
        roomViewModel.loadHouseById(houseId)
    }

    // Count holds as taken; hide rooms closed for maintenance
    val availableRooms = rooms.filter { it.availableCount > 0 && it.status == RoomStatus.AVAILABLE }

    Scaffold(topBar = { NyumbaTopBar(title = house?.name ?: "Rooms", onBack = onNavigateBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            house?.let { h ->
                item {
                    HeroCard {
                        Text(h.name, style = MaterialTheme.typography.headlineSmall)
                        if (h.address.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            IconLine(Icons.Default.LocationOn, h.address)
                        }
                        if (h.phone.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            IconLine(Icons.Default.Phone, h.phone)
                        }
                        if (h.description.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(h.description, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.9f))
                        }
                    }
                }
                if (h.houseRules.isNotBlank()) {
                    item {
                        SurfaceCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(Icons.Default.Gavel, tone = Tone.Warning, size = 36)
                                Spacer(Modifier.width(12.dp))
                                Text("House rules", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(h.houseRules, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { SectionHeader("Available rooms (${availableRooms.size})") }
            if (availableRooms.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.MeetingRoom,
                        title = "Fully booked",
                        message = "There are no free beds here right now. Check back soon or try another house."
                    )
                }
            } else {
                items(availableRooms, key = { it.roomId }) { room ->
                    AvailableRoomCard(room = room, onClick = { onRoomSelected(room.roomId) })
                }
            }
        }
    }
}

@Composable
private fun IconLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AvailableRoomCard(room: Room, onClick: () -> Unit) {
    SurfaceCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Default.MeetingRoom, tone = Tone.Brand)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(room.roomName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${room.availableCount} of ${room.capacity} bed(s) free",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatKwacha(room.monthlyRent), style = MaterialTheme.typography.titleMedium)
                Text("per month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (room.description.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                room.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
