package com.j01000011.nyumba.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.j01000011.nyumba.data.repository.AuthRepository
import com.j01000011.nyumba.domain.model.UserRole
import com.j01000011.nyumba.domain.repository.BoardingHouseRepository
import com.j01000011.nyumba.domain.repository.ComplaintRepository
import com.j01000011.nyumba.domain.repository.RentRepository
import com.j01000011.nyumba.domain.repository.RoomRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Runs on the signed-in user's device, so it only processes that user's own data
 * (security rules reject cross-tenant queries). Tenants handle their own holds and
 * rent reminders; landlords handle holds and complaint reminders for their houses.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val boardingHouseRepository: BoardingHouseRepository,
    private val roomRepository: RoomRepository,
    private val complaintRepository: ComplaintRepository,
    private val rentRepository: RentRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.success()

        return try {
            val user = authRepository.getProfile(uid).getOrThrow()
            val results = when (user.role) {
                UserRole.TENANT -> listOf(
                    // 1. Release this tenant's expired room holds
                    roomRepository.cleanupExpiredHolds(tenantId = uid),
                    // 2. Rent reminders (7d, 3d, today, overdue)
                    rentRepository.checkAndSendRentReminders(uid)
                )
                UserRole.LANDLORD -> boardingHouseRepository.getBoardingHouses(uid).first().flatMap { house ->
                    listOf(
                        roomRepository.cleanupExpiredHolds(boardingHouseId = house.boardingHouseId),
                        // Complaint reminders (24h, 48h, 72h)
                        complaintRepository.checkAndSendComplaintReminders(house.boardingHouseId)
                    )
                }
            }
            if (results.any { it.isFailure }) Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
