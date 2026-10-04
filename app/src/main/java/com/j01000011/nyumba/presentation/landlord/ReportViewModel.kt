package com.j01000011.nyumba.presentation.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.RentPeriod
import com.j01000011.nyumba.domain.model.RentStatus
import com.j01000011.nyumba.domain.repository.RentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinancialReport(
    val totalExpected: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val advancePaidCount: Int = 0,
    val overdueCount: Int = 0
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val rentRepository: RentRepository
) : ViewModel() {

    private val _report = MutableStateFlow(FinancialReport())
    val report: StateFlow<FinancialReport> = _report.asStateFlow()

    private val _periods = MutableStateFlow<List<RentPeriod>>(emptyList())
    val periods: StateFlow<List<RentPeriod>> = _periods.asStateFlow()

    fun loadReportData(boardingHouseId: String) {
        viewModelScope.launch {
            rentRepository.getHouseRentPeriods(boardingHouseId).collectLatest { periods ->
                _periods.value = periods
                
                val totalExpected = periods.sumOf { it.amountDue }
                val totalCollected = periods.sumOf { it.amountPaid }
                val totalOutstanding = totalExpected - totalCollected
                val overdueCount = periods.count { it.status == RentStatus.OVERDUE }
                val advancePaidCount = periods.count { it.status == RentStatus.ADVANCE_PAID }

                _report.value = FinancialReport(
                    totalExpected = totalExpected,
                    totalCollected = totalCollected,
                    totalOutstanding = totalOutstanding,
                    advancePaidCount = advancePaidCount,
                    overdueCount = overdueCount
                )
            }
        }
    }
}
