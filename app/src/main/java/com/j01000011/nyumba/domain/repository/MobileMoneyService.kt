package com.j01000011.nyumba.domain.repository

import com.j01000011.nyumba.domain.model.MobileMoneyPayment
import kotlinx.coroutines.flow.Flow

interface MobileMoneyService {
    suspend fun initiatePayment(phoneNumber: String, amount: Double): Result<MobileMoneyPayment>
    suspend fun checkPaymentStatus(transactionId: String): Result<MobileMoneyPayment>
    fun getPaymentUpdates(transactionId: String): Flow<MobileMoneyPayment>
}
