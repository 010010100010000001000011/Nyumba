package com.j01000011.nyumba.domain.repository

import com.j01000011.nyumba.domain.model.Complaint
import kotlinx.coroutines.flow.Flow

interface ComplaintRepository {
    fun getTenantComplaints(tenantId: String): Flow<List<Complaint>>
    fun getHouseComplaints(boardingHouseId: String): Flow<List<Complaint>>
    suspend fun getComplaintById(complaintId: String): Result<Complaint>
    suspend fun submitComplaint(complaint: Complaint): Result<String>
    suspend fun updateComplaintStatus(complaintId: String, status: String, comment: String? = null): Result<Unit>
    suspend fun checkAndSendComplaintReminders(): Result<Int>
}
