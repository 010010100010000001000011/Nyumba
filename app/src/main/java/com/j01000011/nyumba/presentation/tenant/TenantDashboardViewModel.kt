package com.j01000011.nyumba.presentation.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.*
import com.j01000011.nyumba.domain.repository.BoardingHouseRepository
import com.j01000011.nyumba.domain.repository.RentRepository
import com.j01000011.nyumba.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TenantDashboardStats(
    val currentPeriod: RentPeriod? = null,
    val totalPaid: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val advancePeriodsCovered: Int = 0,
    val nextDueDate: Long? = null,
    val nextDueStatus: RentStatus? = null,
    val requests: List<RoomRequest> = emptyList(),
    val currentRoom: Room? = null,
    val currentHouse: BoardingHouse? = null
)

@HiltViewModel
class TenantDashboardViewModel @Inject constructor(
    private val rentRepository: RentRepository,
    private val roomRepository: RoomRepository,
    private val boardingHouseRepository: BoardingHouseRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(TenantDashboardStats())
    val stats: StateFlow<TenantDashboardStats> = _stats.asStateFlow()

    fun loadDashboardData(user: User) {
        viewModelScope.launch {
            combine(
                rentRepository.getRentPeriods(user.uid),
                roomRepository.getTenantRequests(user.uid)
            ) { periods, requests ->
                val now = System.currentTimeMillis()
                val sortedPeriods = periods.sortedWith(compareBy({ it.year }, { it.month }))
                
                val current = sortedPeriods.find { it.dueDate <= now && it.status != RentStatus.PAID && it.status != RentStatus.ADVANCE_PAID } 
                    ?: sortedPeriods.firstOrNull { it.status == RentStatus.UNPAID || it.status == RentStatus.PARTIALLY_PAID || it.status == RentStatus.OVERDUE }

                val outstanding = sortedPeriods.sumOf { maxOf(0.0, it.amountDue - it.amountPaid) }
                val advanceCount = sortedPeriods.count { it.status == RentStatus.ADVANCE_PAID }
                val nextUnpaid = sortedPeriods.firstOrNull { it.status == RentStatus.UNPAID || it.status == RentStatus.PARTIALLY_PAID }
                
                TenantDashboardStats(
                    currentPeriod = current,
                    totalPaid = sortedPeriods.sumOf { p -> p.amountPaid },
                    outstandingBalance = outstanding,
                    advancePeriodsCovered = advanceCount,
                    nextDueDate = nextUnpaid?.dueDate,
                    nextDueStatus = nextUnpaid?.status,
                    requests = requests
                )
            }.collect { newStats ->
                _stats.update { it.copy(
                    currentPeriod = newStats.currentPeriod,
                    totalPaid = newStats.totalPaid,
                    outstandingBalance = newStats.outstandingBalance,
                    advancePeriodsCovered = newStats.advancePeriodsCovered,
                    nextDueDate = newStats.nextDueDate,
                    nextDueStatus = newStats.nextDueStatus,
                    requests = newStats.requests
                ) }
            }
        }

        if (user.currentRoomId != null && user.boardingHouseId != null) {
            viewModelScope.launch {
                roomRepository.getRoomById(user.currentRoomId).onSuccess { room ->
                    _stats.update { it.copy(currentRoom = room) }
                }
                boardingHouseRepository.getBoardingHouseById(user.boardingHouseId).onSuccess { house ->
                    _stats.update { it.copy(currentHouse = house) }
                }
            }
        }
    }
}
