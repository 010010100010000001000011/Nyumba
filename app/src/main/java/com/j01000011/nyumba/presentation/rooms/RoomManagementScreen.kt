package com.j01000011.nyumba.presentation.rooms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Room
import com.j01000011.nyumba.ui.components.*

@Composable
fun RoomManagementScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddRoom: (String) -> Unit,
    onNavigateToEditRoom: (String) -> Unit,
    onNavigateToRoomRequests: (String) -> Unit,
    viewModel: RoomViewModel = hiltViewModel()
) {
    val rooms by viewModel.rooms.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadRooms(houseId)
    }

    val beds = rooms.sumOf { it.capacity }
    val occupied = rooms.sumOf { it.occupiedCount }
    val free = rooms.sumOf { maxOf(0, it.availableCount) }

    Scaffold(
        topBar = {
            NyumbaTopBar(
                title = "Rooms",
                onBack = onNavigateBack,
                actions = {
                    TextButton(onClick = { onNavigateToRoomRequests(houseId) }) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Requests")
                    }
                }
            )
        },
        floatingActionButton = {
            if (rooms.isNotEmpty()) ExtendedFloatingActionButton(
                onClick = { onNavigateToAddRoom(houseId) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add room") }
            )
        }
    ) { padding ->
        if (rooms.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MeetingRoom,
                title = "No rooms yet",
                message = "Add the rooms in this house so tenants can find and request them.",
                actionLabel = "Add a room",
                onAction = { onNavigateToAddRoom(houseId) },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricTile("Rooms", rooms.size.toString(), Icons.Default.MeetingRoom, Modifier.weight(1f))
                        MetricTile("Beds filled", "$occupied/$beds", Icons.Default.Bed, Modifier.weight(1f), Tone.Info)
                        MetricTile("Free beds", free.toString(), Icons.Default.EventAvailable, Modifier.weight(1f), Tone.Success)
                    }
                }
                items(rooms, key = { it.roomId }) { room ->
                    RoomItemCard(
                        room = room,
                        onEditClick = { onNavigateToEditRoom(room.roomId) },
                        onCapacityChange = { newCap -> viewModel.updateRoomCapacity(room, newCap) }
                    )
                }
            }
        }
    }
}

@Composable
fun RoomItemCard(room: Room, onEditClick: () -> Unit, onCapacityChange: (Int) -> Unit) {
    val fill = if (room.capacity > 0) room.occupiedCount.toFloat() / room.capacity else 0f
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Default.MeetingRoom, tone = room.status.tone())
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(room.roomName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatKwacha(room.monthlyRent)} / month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RoomStatusPill(room.status)
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${room.roomName}")
            }
        }
        if (room.description.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                room.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { fill },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            strokeCap = StrokeCap.Round,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildString {
                    append("${room.occupiedCount} of ${room.capacity} beds filled")
                    if (room.heldCount > 0) append(" · ${room.heldCount} on hold")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            // Capacity stepper; cannot go below current occupants
            FilledTonalIconButton(
                onClick = { onCapacityChange(room.capacity - 1) },
                enabled = room.capacity - 1 >= maxOf(1, room.occupiedCount),
                modifier = Modifier.size(32.dp)
            ) { Icon(Icons.Default.Remove, contentDescription = "Remove a bed", modifier = Modifier.size(18.dp)) }
            Text("${room.capacity}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 10.dp))
            FilledTonalIconButton(
                onClick = { onCapacityChange(room.capacity + 1) },
                modifier = Modifier.size(32.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Add a bed", modifier = Modifier.size(18.dp)) }
        }
    }
}
