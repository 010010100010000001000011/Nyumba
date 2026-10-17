package com.j01000011.nyumba.presentation.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.Payment
import com.j01000011.nyumba.domain.model.RentPeriod
import com.j01000011.nyumba.domain.model.RentStatus
import com.j01000011.nyumba.domain.repository.RentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinancialReport(
    val totalExpected: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val advancePaidCount: Int = 0,
    val overdueCount: Int = 0,
    val recentPayments: List<Payment> = emptyList()
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
            combine(
                rentRepository.getHouseRentPeriods(boardingHouseId),
                rentRepository.getLandlordPayments(boardingHouseId)
            ) { periods, payments ->
                _periods.value = periods
                
                val currentOrPastPeriods = periods.filter { it.isCurrentOrPastDue }
                val totalExpected = currentOrPastPeriods.sumOf { it.amountDue }
                val totalCollected = periods.sumOf { it.amountPaid }
                val totalOutstanding = currentOrPastPeriods.sumOf { maxOf(0.0, it.amountDue - it.amountPaid) }
                val overdueCount = periods.count { it.status == RentStatus.OVERDUE }
                val advancePaidCount = periods.count { it.status == RentStatus.ADVANCE_PAID }

                FinancialReport(
                    totalExpected = totalExpected,
                    totalCollected = totalCollected,
                    totalOutstanding = totalOutstanding,
                    advancePaidCount = advancePaidCount,
                    overdueCount = overdueCount,
                    recentPayments = payments
                )
            }.collectLatest { report ->
                _report.value = report
            }
        }
    }
}
