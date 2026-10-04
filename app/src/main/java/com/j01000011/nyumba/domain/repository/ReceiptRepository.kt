package com.j01000011.nyumba.domain.repository

import com.j01000011.nyumba.domain.model.Receipt
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    fun getTenantReceipts(tenantId: String): Flow<List<Receipt>>
    fun getHouseReceipts(boardingHouseId: String): Flow<List<Receipt>>
    suspend fun getReceiptById(receiptId: String): Result<Receipt>
}
