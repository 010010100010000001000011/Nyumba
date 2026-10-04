package com.j01000011.nyumba.domain.repository

import com.j01000011.nyumba.domain.model.User
import kotlinx.coroutines.flow.Flow

interface TenantRepository {
    fun getTenants(boardingHouseId: String): Flow<List<User>>
    suspend fun getTenantById(tenantId: String): Result<User>
    suspend fun updateTenantConfig(tenantId: String, monthlyRent: Double, dueDay: Int): Result<Unit>
    suspend fun moveOutTenant(tenantId: String): Result<Unit>
    suspend fun deleteTenantRecords(tenantId: String): Result<Unit>
}
