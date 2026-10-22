package com.j01000011.nyumba.presentation.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.*
import com.j01000011.nyumba.domain.repository.MobileMoneyService
import com.j01000011.nyumba.domain.repository.RentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RentUiState {
    object Idle : RentUiState()
    object Loading : RentUiState()
    object Success : RentUiState()
    data class Error(val message: String) : RentUiState()
    data class AwaitingPayment(val transactionId: String, val phoneNumber: String, val amount: Double) : RentUiState()
    /** Mobile money confirmed by Lipila; the backend has already recorded the payment and receipt. */
    data class PaymentConfirmed(val amount: Double, val receiptId: String?) : RentUiState()
}

@HiltViewModel
class RentViewModel @Inject constructor(
    private val rentRepository: RentRepository,
    private val mobileMoneyService: MobileMoneyService
) : ViewModel() {

    private val _rentPeriods = MutableStateFlow<List<RentPeriod>>(emptyList())
    val rentPeriods: StateFlow<List<RentPeriod>> = _rentPeriods.asStateFlow()

    private val _paymentHistory = MutableStateFlow<List<Payment>>(emptyList())
    val paymentHistory: StateFlow<List<Payment>> = _paymentHistory.asStateFlow()

    private val _rentState = MutableStateFlow<RentUiState>(RentUiState.Idle)
    val rentState: StateFlow<RentUiState> = _rentState.asStateFlow()

    fun loadTenantRentData(tenantId: String) {
        viewModelScope.launch {
            rentRepository.getRentPeriods(tenantId)
                .catch { emit(emptyList()) }
                .collectLatest {
                    _rentPeriods.value = it
                }
        }
        viewModelScope.launch {
            rentRepository.getPaymentHistory(tenantId)
                .catch { emit(emptyList()) }
                .collectLatest {
                    _paymentHistory.value = it
                }
        }
    }

    fun loadLandlordPayments(boardingHouseId: String) {
        viewModelScope.launch {
            rentRepository.getLandlordPayments(boardingHouseId)
                .catch { emit(emptyList()) }
                .collectLatest {
                    _paymentHistory.value = it
                }
        }
    }

    fun makePayment(phoneNumber: String, amount: Double, notes: String? = null) {
        viewModelScope.launch {
            _rentState.value = RentUiState.Loading
            mobileMoneyService.initiatePayment(phoneNumber, amount, notes)
                .onSuccess { payment ->
                    _rentState.value = RentUiState.AwaitingPayment(payment.transactionId, phoneNumber, amount)
                    observeMobileMoneyStatus(payment.transactionId, amount)
                }
                .onFailure {
                    _rentState.value = RentUiState.Error(it.message ?: "Failed to start mobile money payment")
                }
        }
    }

    private fun observeMobileMoneyStatus(transactionId: String, amount: Double) {
        viewModelScope.launch {
            mobileMoneyService.getPaymentUpdates(transactionId)
                .catch { _rentState.value = RentUiState.Error(it.message ?: "Lost track of the payment status") }
                .collect { update ->
                    when (update.status) {
                        MobileMoneyStatus.SUCCESSFUL ->
                            _rentState.value = RentUiState.PaymentConfirmed(amount, update.receiptId)
                        MobileMoneyStatus.FAILED, MobileMoneyStatus.CANCELLED ->
                            _rentState.value = RentUiState.Error(update.message ?: "Payment ${update.status.name.lowercase()}")
                        MobileMoneyStatus.PENDING -> Unit // keep waiting
                    }
                }
        }
    }

    fun recordCashPayment(
        tenantId: String,
        landlordId: String,
        boardingHouseId: String,
        roomId: String,
        amount: Double,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _rentState.value = RentUiState.Loading
            val payment = Payment(
                tenantId = tenantId,
                landlordId = landlordId,
                boardingHouseId = boardingHouseId,
                roomId = roomId,
                amount = amount,
                paymentMethod = PaymentMethod.CASH,
                notes = notes,
                createdBy = landlordId
            )
            rentRepository.recordPayment(payment)
                .onSuccess { _rentState.value = RentUiState.Success }
                .onFailure { _rentState.value = RentUiState.Error(it.message ?: "Failed to record cash payment") }
        }
    }

    fun clearState() {
        _rentState.value = RentUiState.Idle
    }
}
