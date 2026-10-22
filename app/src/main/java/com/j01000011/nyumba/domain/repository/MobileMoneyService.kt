package com.j01000011.nyumba.domain.repository

import com.j01000011.nyumba.domain.model.MobileMoneyPayment
import kotlinx.coroutines.flow.Flow

interface MobileMoneyService {
    /** Sends a payment prompt to [phoneNumber]; the result's transactionId identifies the payment. */
    suspend fun initiatePayment(phoneNumber: String, amount: Double, notes: String? = null): Result<MobileMoneyPayment>
    suspend fun checkPaymentStatus(transactionId: String): Result<MobileMoneyPayment>
    /** Emits until the payment leaves PENDING. */
    fun getPaymentUpdates(transactionId: String): Flow<MobileMoneyPayment>
}
