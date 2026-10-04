package com.j01000011.nyumba.presentation.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.Receipt
import com.j01000011.nyumba.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReceiptUiState {
    object Idle : ReceiptUiState()
    object Loading : ReceiptUiState()
    data class Error(val message: String) : ReceiptUiState()
}

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _receipts = MutableStateFlow<List<Receipt>>(emptyList())
    val receipts: StateFlow<List<Receipt>> = _receipts.asStateFlow()

    private val _selectedReceipt = MutableStateFlow<Receipt?>(null)
    val selectedReceipt: StateFlow<Receipt?> = _selectedReceipt.asStateFlow()

    private val _uiState = MutableStateFlow<ReceiptUiState>(ReceiptUiState.Idle)
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    fun loadTenantReceipts(tenantId: String) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.Loading
            receiptRepository.getTenantReceipts(tenantId)
                .catch { e ->
                    _uiState.value = ReceiptUiState.Error(e.message ?: "Failed to load receipts")
                }
                .collectLatest {
                    _receipts.value = it
                    _uiState.value = ReceiptUiState.Idle
                }
        }
    }

    fun loadHouseReceipts(houseId: String) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.Loading
            receiptRepository.getHouseReceipts(houseId)
                .catch { e ->
                    _uiState.value = ReceiptUiState.Error(e.message ?: "Failed to load receipts")
                }
                .collectLatest {
                    _receipts.value = it
                    _uiState.value = ReceiptUiState.Idle
                }
        }
    }

    fun loadReceiptById(receiptId: String) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.Loading
            receiptRepository.getReceiptById(receiptId)
                .onSuccess {
                    _selectedReceipt.value = it
                    _uiState.value = ReceiptUiState.Idle
                }
                .onFailure {
                    _uiState.value = ReceiptUiState.Error(it.message ?: "Failed to load receipt")
                }
        }
    }

    fun clearState() {
        _uiState.value = ReceiptUiState.Idle
    }
}
