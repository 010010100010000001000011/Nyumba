package com.j01000011.nyumba.presentation.complaints

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.core.util.ImageStorageService
import com.j01000011.nyumba.domain.model.Complaint
import com.j01000011.nyumba.domain.model.ComplaintCategory
import com.j01000011.nyumba.domain.model.ComplaintStatus
import com.j01000011.nyumba.domain.repository.ComplaintRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ComplaintUiState {
    object Idle : ComplaintUiState()
    object Loading : ComplaintUiState()
    object Success : ComplaintUiState()
    data class Error(val message: String) : ComplaintUiState()
}

@HiltViewModel
class ComplaintViewModel @Inject constructor(
    private val complaintRepository: ComplaintRepository,
    private val imageStorageService: ImageStorageService
) : ViewModel() {

    private val _complaints = MutableStateFlow<List<Complaint>>(emptyList())
    val complaints: StateFlow<List<Complaint>> = _complaints.asStateFlow()

    private val _selectedComplaint = MutableStateFlow<Complaint?>(null)
    val selectedComplaint: StateFlow<Complaint?> = _selectedComplaint.asStateFlow()

    private val _uiState = MutableStateFlow<ComplaintUiState>(ComplaintUiState.Idle)
    val uiState: StateFlow<ComplaintUiState> = _uiState.asStateFlow()

    fun loadTenantComplaints(tenantId: String) {
        viewModelScope.launch {
            complaintRepository.getTenantComplaints(tenantId).collectLatest {
                _complaints.value = it
            }
        }
    }

    fun loadHouseComplaints(houseId: String) {
        viewModelScope.launch {
            complaintRepository.getHouseComplaints(houseId).collectLatest {
                _complaints.value = it
            }
        }
    }

    fun loadComplaintById(complaintId: String) {
        viewModelScope.launch {
            _uiState.value = ComplaintUiState.Loading
            complaintRepository.getComplaintById(complaintId)
                .onSuccess {
                    _selectedComplaint.value = it
                    _uiState.value = ComplaintUiState.Idle
                }
                .onFailure {
                    _uiState.value = ComplaintUiState.Error(it.message ?: "Failed to load complaint")
                }
        }
    }

    fun submitComplaint(
        tenantId: String,
        landlordId: String,
        boardingHouseId: String,
        title: String,
        description: String,
        category: ComplaintCategory,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            _uiState.value = ComplaintUiState.Loading
            
            val localPath = imageUri?.let { imageStorageService.saveImageLocally(it) }
            
            val complaint = Complaint(
                tenantId = tenantId,
                landlordId = landlordId,
                boardingHouseId = boardingHouseId,
                title = title,
                description = description,
                category = category,
                localImagePath = localPath
            )
            complaintRepository.submitComplaint(complaint)
                .onSuccess { _uiState.value = ComplaintUiState.Success }
                .onFailure { _uiState.value = ComplaintUiState.Error(it.message ?: "Failed to submit complaint") }
        }
    }

    fun resolveComplaint(complaintId: String, comment: String?) {
        viewModelScope.launch {
            _uiState.value = ComplaintUiState.Loading
            complaintRepository.updateComplaintStatus(complaintId, ComplaintStatus.RESOLVED.name, comment)
                .onSuccess { _uiState.value = ComplaintUiState.Success }
                .onFailure { _uiState.value = ComplaintUiState.Error(it.message ?: "Failed to resolve complaint") }
        }
    }

    fun clearState() {
        _uiState.value = ComplaintUiState.Idle
    }
}
