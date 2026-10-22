package com.j01000011.nyumba.presentation.rooms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.j01000011.nyumba.domain.model.RoomStatus
import com.j01000011.nyumba.ui.components.*

fun RoomStatus.tone(): Tone = when (this) {
    RoomStatus.AVAILABLE -> Tone.Success
    RoomStatus.FULL -> Tone.Info
    RoomStatus.MAINTENANCE -> Tone.Warning
    RoomStatus.INACTIVE -> Tone.Neutral
}

@Composable
fun RoomStatusPill(status: RoomStatus) = StatusPill(humanize(status.name), status.tone())

data class RoomFormValues(
    val name: String = "",
    val capacity: String = "1",
    val rent: String = "",
    val description: String = "",
    val status: RoomStatus = RoomStatus.AVAILABLE
)

/** Form shared by Add room and Edit room. [minCapacity] stops capacity dropping below current occupants. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomForm(
    initial: RoomFormValues,
    submitLabel: String,
    loading: Boolean,
    error: String?,
    showStatus: Boolean,
    minCapacity: Int,
    modifier: Modifier = Modifier,
    onSubmit: (name: String, capacity: Int, rent: Double, description: String, status: RoomStatus) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var capacity by remember(initial) { mutableStateOf(initial.capacity) }
    var rent by remember(initial) { mutableStateOf(initial.rent) }
    var description by remember(initial) { mutableStateOf(initial.description) }
    var status by remember(initial) { mutableStateOf(initial.status) }
    var submitted by remember { mutableStateOf(false) }

    val capacityValue = capacity.toIntOrNull()
    val rentValue = rent.toDoubleOrNull()
    val nameError = submitted && name.isBlank()
    val capacityError = submitted && (capacityValue == null || capacityValue < maxOf(1, minCapacity))
    val rentError = submitted && (rentValue == null || rentValue <= 0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NyumbaTextField(
            value = name, onValueChange = { name = it }, label = "Room name or number",
            leadingIcon = Icons.Default.MeetingRoom, placeholder = "e.g. Room 4B",
            isError = nameError, supportingText = if (nameError) "Give the room a name" else null
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NyumbaTextField(
                value = capacity,
                onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) capacity = it },
                label = "Beds",
                leadingIcon = Icons.Default.People,
                keyboardType = KeyboardType.Number,
                isError = capacityError,
                supportingText = when {
                    capacityError && minCapacity > 0 -> "At least $minCapacity (current tenants)"
                    capacityError -> "At least 1"
                    else -> null
                },
                modifier = Modifier.weight(1f)
            )
            NyumbaTextField(
                value = rent,
                onValueChange = { if (it.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?$"))) rent = it },
                label = "Rent / month",
                prefix = "K ",
                keyboardType = KeyboardType.Decimal,
                isError = rentError,
                supportingText = if (rentError) "Enter the rent" else null,
                modifier = Modifier.weight(1f)
            )
        }
        NyumbaTextField(
            value = description, onValueChange = { description = it },
            label = "Description (optional)", placeholder = "Furnishing, shared bathroom, Wi-Fi...",
            singleLine = false, minLines = 3
        )
        if (showStatus) {
            SectionHeader("Status")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoomStatus.entries.forEach { s ->
                    FilterChip(selected = status == s, onClick = { status = s }, label = { Text(humanize(s.name)) })
                }
            }
        }
        error?.let { InlineMessage(text = it, icon = Icons.Default.ErrorOutline) }
        PrimaryButton(
            text = submitLabel,
            loading = loading,
            icon = Icons.Default.Save,
            onClick = {
                submitted = true
                if (name.isNotBlank() && capacityValue != null && capacityValue >= maxOf(1, minCapacity) && rentValue != null && rentValue > 0) {
                    onSubmit(name.trim(), capacityValue, rentValue, description.trim(), status)
                }
            }
        )
        Spacer(Modifier.height(8.dp))
    }
}
