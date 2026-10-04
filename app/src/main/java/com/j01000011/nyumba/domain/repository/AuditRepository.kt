package com.j01000011.nyumba.domain.repository

import com.j01000011.nyumba.domain.model.AuditLog
import kotlinx.coroutines.flow.Flow

interface AuditRepository {
    fun getAuditLogs(boardingHouseId: String): Flow<List<AuditLog>>
    suspend fun recordLog(log: AuditLog): Result<String>
}
