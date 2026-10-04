package com.j01000011.nyumba.data.payment

import com.j01000011.nyumba.domain.model.MobileMoneyPayment
import com.j01000011.nyumba.domain.model.MobileMoneyStatus
import com.j01000011.nyumba.domain.repository.MobileMoneyService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockMobileMoneyService @Inject constructor() : MobileMoneyService {

    private val payments = mutableMapOf<String, MobileMoneyPayment>()

    override suspend fun initiatePayment(phoneNumber: String, amount: Double): Result<MobileMoneyPayment> {
        val transactionId = UUID.randomUUID().toString()
        val payment = MobileMoneyPayment(
            transactionId = transactionId,
            phoneNumber = phoneNumber,
            amount = amount,
            status = MobileMoneyStatus.PENDING
        )
        payments[transactionId] = payment
        return Result.success(payment)
    }

    override suspend fun checkPaymentStatus(transactionId: String): Result<MobileMoneyPayment> {
        val payment = payments[transactionId] ?: return Result.failure(Exception("Payment not found"))
        
        // Mocking logic: if it's pending, randomly succeed or stay pending
        if (payment.status == MobileMoneyStatus.PENDING) {
            val random = Random().nextInt(10)
            if (random > 7) {
                val updated = payment.copy(status = MobileMoneyStatus.SUCCESSFUL)
                payments[transactionId] = updated
                return Result.success(updated)
            }
        }
        
        return Result.success(payment)
    }

    override fun getPaymentUpdates(transactionId: String): Flow<MobileMoneyPayment> = flow {
        var currentStatus = MobileMoneyStatus.PENDING
        while (currentStatus == MobileMoneyStatus.PENDING) {
            val result = checkPaymentStatus(transactionId)
            result.onSuccess {
                emit(it)
                currentStatus = it.status
            }
            delay(3000) // Poll every 3 seconds
        }
    }
}
