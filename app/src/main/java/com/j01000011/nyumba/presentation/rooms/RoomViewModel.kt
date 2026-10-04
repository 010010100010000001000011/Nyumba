package com.j01000011.nyumba.presentation.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.*
import com.j01000011.nyumba.domain.repository.BoardingHouseRepository
import com.j01000011.nyumba.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RoomUiState {
    object Idle : RoomUiState()
    object Loading : RoomUiState()
    object Success : RoomUiState()
    data class Error(val message: String) : RoomUiState()
}

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val boardingHouseRepository: BoardingHouseRepository
) : ViewModel() {

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    private val _availableRooms = MutableStateFlow<List<Room>>(emptyList())
    val availableRooms: StateFlow<List<Room>> = _availableRooms.asStateFlow()

    private val _roomRequests = MutableStateFlow<List<RoomRequest>>(emptyList())
    val roomRequests: StateFlow<List<RoomRequest>> = _roomRequests.asStateFlow()

    private val _selectedRoom = MutableStateFlow<Room?>(null)
    val selectedRoom: StateFlow<Room?> = _selectedRoom.asStateFlow()

    private val _selectedHouse = MutableStateFlow<BoardingHouse?>(null)
    val selectedHouse: StateFlow<BoardingHouse?> = _selectedHouse.asStateFlow()

    private val _roomState = MutableStateFlow<RoomUiState>(RoomUiState.Idle)
    val roomState: StateFlow<RoomUiState> = _roomState.asStateFlow()

    fun loadRooms(boardingHouseId: String) {
        viewModelScope.launch {
            roomRepository.getRooms(boardingHouseId).collectLatest { roomList ->
                _rooms.value = roomList
            }
        }
    }

    fun loadAvailableRooms() {
        viewModelScope.launch {
            roomRepository.getAvailableRooms().collectLatest { roomList ->
                _availableRooms.value = roomList
            }
        }
    }

    fun loadRoomById(roomId: String) {
        viewModelScope.launch {
            _roomState.value = RoomUiState.Loading
            roomRepository.getRoomById(roomId)
                .onSuccess { room ->
                    _selectedRoom.value = room
                    _roomState.value = RoomUiState.Idle
                    loadHouseById(room.boardingHouseId)
                }
                .onFailure {
                    _roomState.value = RoomUiState.Error(it.message ?: "Failed to load room")
                }
        }
    }

    private fun loadHouseById(houseId: String) {
        viewModelScope.launch {
            boardingHouseRepository.getBoardingHouseById(houseId)
                .onSuccess { _selectedHouse.value = it }
        }
    }

    fun loadRoomRequests(boardingHouseId: String) {
        viewModelScope.launch {
            roomRepository.getRoomRequests(boardingHouseId).collectLatest { requestList ->
                _roomRequests.value = requestList
            }
        }
    }

    fun loadTenantRequests(tenantId: String) {
        viewModelScope.launch {
            roomRepository.getTenantRequests(tenantId).collectLatest { requestList ->
                _roomRequests.value = requestList
            }
        }
    }

    fun createRoom(
        boardingHouseId: String,
        roomName: String,
        capacity: Int,
        monthlyRent: Double,
        description: String
    ) {
        viewModelScope.launch {
            _roomState.value = RoomUiState.Loading
            val room = Room(
                boardingHouseId = boardingHouseId,
                roomName = roomName,
                capacity = capacity,
                monthlyRent = monthlyRent,
                description = description,
                status = RoomStatus.AVAILABLE
            )
            roomRepository.createRoom(room)
                .onSuccess { _roomState.value = RoomUiState.Success }
                .onFailure { _roomState.value = RoomUiState.Error(it.message ?: "Failed to create room") }
        }
    }

    fun updateRoom(room: Room) {
        viewModelScope.launch {
            _roomState.value = RoomUiState.Loading
            roomRepository.updateRoom(room)
                .onSuccess { _roomState.value = RoomUiState.Success }
                .onFailure { _roomState.value = RoomUiState.Error(it.message ?: "Failed to update room") }
        }
    }

    fun updateRoomCapacity(room: Room, newCapacity: Int) {
        if (newCapacity < room.occupiedCount) return 
        viewModelScope.launch {
            val updatedStatus = if (room.occupiedCount >= newCapacity) RoomStatus.FULL else RoomStatus.AVAILABLE
            val updatedRoom = room.copy(capacity = newCapacity, status = updatedStatus)
            roomRepository.updateRoom(updatedRoom)
        }
    }

    fun requestRoom(roomId: String, tenantId: String, tenantName: String, boardingHouseId: String) {
        viewModelScope.launch {
            _roomState.value = RoomUiState.Loading
            val holdDuration = 30 * 60 * 1000L 
            roomRepository.createRoomHold(roomId, tenantId, tenantName, boardingHouseId, holdDuration)
                .onSuccess { _roomState.value = RoomUiState.Success }
                .onFailure { _roomState.value = RoomUiState.Error(it.message ?: "Failed to request room") }
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            _roomState.value = RoomUiState.Loading
            roomRepository.approveRoomRequest(requestId)
                .onSuccess { _roomState.value = RoomUiState.Success }
                .onFailure { _roomState.value = RoomUiState.Error(it.message ?: "Approval failed") }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            _roomState.value = RoomUiState.Loading
            roomRepository.updateRoomRequestStatus(requestId, RoomRequestStatus.REJECTED.name)
                .onSuccess { _roomState.value = RoomUiState.Success }
                .onFailure { _roomState.value = RoomUiState.Error(it.message ?: "Rejection failed") }
        }
    }

    fun changeRoom(tenantId: String, oldRoomId: String?, newRoomId: String) {
        viewModelScope.launch {
            _roomState.value = RoomUiState.Loading
            roomRepository.changeTenantRoom(tenantId, oldRoomId, newRoomId)
                .onSuccess { _roomState.value = RoomUiState.Success }
                .onFailure { _roomState.value = RoomUiState.Error(it.message ?: "Room change failed") }
        }
    }
    
    fun clearState() {
        _roomState.value = RoomUiState.Idle
    }
}
