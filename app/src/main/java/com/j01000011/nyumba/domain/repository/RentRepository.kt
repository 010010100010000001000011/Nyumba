package com.j01000011.nyumba.domain.repository

import com.j01000011.nyumba.domain.model.Payment
import com.j01000011.nyumba.domain.model.RentPeriod
import com.j01000011.nyumba.domain.model.User
import kotlinx.coroutines.flow.Flow

interface RentRepository {
    fun getRentPeriods(tenantId: String): Flow<List<RentPeriod>>
    fun getHouseRentPeriods(boardingHouseId: String): Flow<List<RentPeriod>>
    fun getPaymentHistory(tenantId: String): Flow<List<Payment>>
    fun getLandlordPayments(boardingHouseId: String): Flow<List<Payment>>
    suspend fun recordPayment(payment: Payment): Result<Unit>
    suspend fun generateInitialRentPeriods(tenant: User): Result<Unit>
    suspend fun checkAndSendRentReminders(tenantId: String): Result<Int>
    /**
     * Re-prices and re-dates the tenant's untouched future periods (nothing paid yet) after
     * their rent or due day changes, so the schedule matches the new terms.
     */
    suspend fun rescheduleFuturePeriods(tenantId: String, boardingHouseId: String, monthlyRent: Double, dueDay: Int): Result<Int>
}
