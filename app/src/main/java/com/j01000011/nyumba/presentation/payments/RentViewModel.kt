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
    data class AwaitingPayment(val transactionId: String) : RentUiState()
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

    fun makePayment(
        tenant: User,
        amount: Double,
        method: PaymentMethod,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _rentState.value = RentUiState.Loading
            
            if (method == PaymentMethod.MOBILE_MONEY) {
                mobileMoneyService.initiatePayment(tenant.phoneNumber, amount)
                    .onSuccess { mmPayment ->
                        _rentState.value = RentUiState.AwaitingPayment(mmPayment.transactionId)
                        // Start observing updates
                        observeMobileMoneyStatus(mmPayment.transactionId, tenant, amount, notes)
                    }
                    .onFailure {
                        _rentState.value = RentUiState.Error(it.message ?: "Failed to initiate Mobile Money")
                    }
            } else {
                // Should not happen for tenants in standard flow unless cash is allowed for them
                _rentState.value = RentUiState.Error("Unsupported payment method for tenant initiation")
            }
        }
    }

    private fun observeMobileMoneyStatus(transactionId: String, tenant: User, amount: Double, notes: String?) {
        viewModelScope.launch {
            mobileMoneyService.getPaymentUpdates(transactionId).collect { update ->
                when (update.status) {
                    MobileMoneyStatus.SUCCESSFUL -> {
                        val payment = Payment(
                            tenantId = tenant.uid,
                            landlordId = tenant.landlordId ?: "",
                            boardingHouseId = tenant.boardingHouseId ?: "",
                            roomId = tenant.currentRoomId ?: "",
                            amount = amount,
                            paymentMethod = PaymentMethod.MOBILE_MONEY,
                            transactionId = transactionId,
                            notes = notes,
                            createdBy = tenant.uid
                        )
                        rentRepository.recordPayment(payment)
                            .onSuccess { _rentState.value = RentUiState.Success }
                            .onFailure { _rentState.value = RentUiState.Error(it.message ?: "Failed to record confirmed payment") }
                    }
                    MobileMoneyStatus.FAILED, MobileMoneyStatus.CANCELLED -> {
                        _rentState.value = RentUiState.Error("Payment ${update.status.name.lowercase()}")
                    }
                    MobileMoneyStatus.PENDING -> {
                        // Continue waiting
                    }
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
