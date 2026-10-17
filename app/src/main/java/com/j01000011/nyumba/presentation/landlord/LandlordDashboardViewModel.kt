package com.j01000011.nyumba.presentation.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.ComplaintStatus
import com.j01000011.nyumba.domain.model.Payment
import com.j01000011.nyumba.domain.model.RentStatus
import com.j01000011.nyumba.domain.model.RoomRequestStatus
import com.j01000011.nyumba.domain.repository.ComplaintRepository
import com.j01000011.nyumba.domain.repository.RentRepository
import com.j01000011.nyumba.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DashboardUiState {
    object Idle : DashboardUiState()
    object Loading : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

data class DashboardStats(
    val totalRooms: Int = 0,
    val totalCapacity: Int = 0,
    val occupiedSpaces: Int = 0,
    val pendingRequests: Int = 0,
    val pendingComplaints: Int = 0,
    val overdueComplaints: Int = 0,
    val rentCollected: Double = 0.0,
    val outstandingRent: Double = 0.0,
    val advancePaidTenants: Int = 0,
    val recentPayments: List<Payment> = emptyList()
)

@HiltViewModel
class LandlordDashboardViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val complaintRepository: ComplaintRepository,
    private val rentRepository: RentRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Idle)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadStats(boardingHouseId: String) {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            combine(
                roomRepository.getRooms(boardingHouseId),
                roomRepository.getRoomRequests(boardingHouseId),
                complaintRepository.getHouseComplaints(boardingHouseId),
                rentRepository.getHouseRentPeriods(boardingHouseId),
                rentRepository.getLandlordPayments(boardingHouseId)
            ) { rooms, requests, complaints, rentPeriods, payments ->
                val totalCapacity = rooms.sumOf { it.capacity }
                val occupied = rooms.sumOf { it.occupiedCount }
                val pendingReq = requests.count { it.status == RoomRequestStatus.PENDING || it.status == RoomRequestStatus.HOLDING }
                val pendingComp = complaints.count { it.status == ComplaintStatus.PENDING }
                val overdueComp = complaints.count { it.status == ComplaintStatus.OVERDUE }
                
                val collected = rentPeriods.sumOf { it.amountPaid }
                val currentOrPastPeriods = rentPeriods.filter { it.isCurrentOrPastDue }
                val outstanding = currentOrPastPeriods.sumOf { maxOf(0.0, it.amountDue - it.amountPaid) }
                
                val advanceTenants = rentPeriods.filter { it.status == RentStatus.ADVANCE_PAID }
                    .map { it.tenantId }.distinct().size

                DashboardStats(
                    totalRooms = rooms.size,
                    totalCapacity = totalCapacity,
                    occupiedSpaces = occupied,
                    pendingRequests = pendingReq,
                    pendingComplaints = pendingComp,
                    overdueComplaints = overdueComp,
                    rentCollected = collected,
                    outstandingRent = outstanding,
                    advancePaidTenants = advanceTenants,
                    recentPayments = payments.take(5)
                )
            }.catch { e ->
                _uiState.value = DashboardUiState.Error(e.message ?: "Failed to load dashboard statistics")
            }.collectLatest {
                _stats.value = it
                _uiState.value = DashboardUiState.Idle
            }
        }
    }

    fun clearState() {
        _uiState.value = DashboardUiState.Idle
    }
}
