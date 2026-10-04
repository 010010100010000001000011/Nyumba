package com.j01000011.nyumba.presentation.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.AuditLog
import com.j01000011.nyumba.domain.repository.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuditLogUiState {
    object Idle : AuditLogUiState()
    object Loading : AuditLogUiState()
    data class Error(val message: String) : AuditLogUiState()
}

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _logs = MutableStateFlow<List<AuditLog>>(emptyList())
    val logs: StateFlow<List<AuditLog>> = _logs.asStateFlow()

    private val _uiState = MutableStateFlow<AuditLogUiState>(AuditLogUiState.Idle)
    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    fun loadLogs(boardingHouseId: String) {
        viewModelScope.launch {
            _uiState.value = AuditLogUiState.Loading
            auditRepository.getAuditLogs(boardingHouseId)
                .catch { e ->
                    _uiState.value = AuditLogUiState.Error(e.message ?: "Failed to load audit logs")
                }
                .collectLatest {
                    _logs.value = it
                    _uiState.value = AuditLogUiState.Idle
                }
        }
    }

    fun clearState() {
        _uiState.value = AuditLogUiState.Idle
    }
}
