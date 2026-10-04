package com.j01000011.nyumba.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.j01000011.nyumba.domain.repository.ComplaintRepository
import com.j01000011.nyumba.domain.repository.RentRepository
import com.j01000011.nyumba.domain.repository.RoomRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val roomRepository: RoomRepository,
    private val complaintRepository: ComplaintRepository,
    private val rentRepository: RentRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Release expired room holds
            roomRepository.cleanupExpiredHolds()
            
            // 2. Check for complaint reminders (24h, 48h, 72h)
            complaintRepository.checkAndSendComplaintReminders()
            
            // 3. Rent reminders (7d, 3d, today, overdue)
            rentRepository.checkAndSendRentReminders()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
