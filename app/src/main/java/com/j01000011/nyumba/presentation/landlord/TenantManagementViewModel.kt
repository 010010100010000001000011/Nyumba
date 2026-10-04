package com.j01000011.nyumba.presentation.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.User
import com.j01000011.nyumba.domain.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TenantUiState {
    object Idle : TenantUiState()
    object Loading : TenantUiState()
    object Success : TenantUiState()
    data class Error(val message: String) : TenantUiState()
}

@HiltViewModel
class TenantManagementViewModel @Inject constructor(
    private val tenantRepository: TenantRepository
) : ViewModel() {

    private val _tenants = MutableStateFlow<List<User>>(emptyList())
    val tenants: StateFlow<List<User>> = _tenants.asStateFlow()

    private val _selectedTenant = MutableStateFlow<User?>(null)
    val selectedTenant: StateFlow<User?> = _selectedTenant.asStateFlow()

    private val _uiState = MutableStateFlow<TenantUiState>(TenantUiState.Idle)
    val uiState: StateFlow<TenantUiState> = _uiState.asStateFlow()

    fun loadTenants(boardingHouseId: String) {
        viewModelScope.launch {
            tenantRepository.getTenants(boardingHouseId).collectLatest {
                _tenants.value = it
            }
        }
    }

    fun loadTenantById(tenantId: String) {
        viewModelScope.launch {
            _uiState.value = TenantUiState.Loading
            tenantRepository.getTenantById(tenantId)
                .onSuccess {
                    _selectedTenant.value = it
                    _uiState.value = TenantUiState.Idle
                }
                .onFailure {
                    _uiState.value = TenantUiState.Error(it.message ?: "Failed to load tenant")
                }
        }
    }

    fun updateTenantConfig(tenantId: String, monthlyRent: Double, dueDay: Int) {
        viewModelScope.launch {
            _uiState.value = TenantUiState.Loading
            tenantRepository.updateTenantConfig(tenantId, monthlyRent, dueDay)
                .onSuccess { _uiState.value = TenantUiState.Success }
                .onFailure { _uiState.value = TenantUiState.Error(it.message ?: "Failed to update tenant") }
        }
    }

    fun moveOutTenant(tenantId: String) {
        viewModelScope.launch {
            _uiState.value = TenantUiState.Loading
            tenantRepository.moveOutTenant(tenantId)
                .onSuccess { _uiState.value = TenantUiState.Success }
                .onFailure { _uiState.value = TenantUiState.Error(it.message ?: "Failed to process move-out") }
        }
    }

    fun deleteTenantRecords(tenantId: String) {
        viewModelScope.launch {
            _uiState.value = TenantUiState.Loading
            tenantRepository.deleteTenantRecords(tenantId)
                .onSuccess { _uiState.value = TenantUiState.Success }
                .onFailure { _uiState.value = TenantUiState.Error(it.message ?: "Failed to delete tenant records") }
        }
    }

    fun clearState() {
        _uiState.value = TenantUiState.Idle
    }
}
