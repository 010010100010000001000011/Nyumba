package com.j01000011.nyumba.di

import com.j01000011.nyumba.data.payment.MockMobileMoneyService
import com.j01000011.nyumba.data.repository.*
import com.j01000011.nyumba.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindBoardingHouseRepository(
        boardingHouseRepositoryImpl: BoardingHouseRepositoryImpl
    ): BoardingHouseRepository

    @Binds
    @Singleton
    abstract fun bindRoomRepository(
        roomRepositoryImpl: RoomRepositoryImpl
    ): RoomRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindRentRepository(
        rentRepositoryImpl: RentRepositoryImpl
    ): RentRepository

    @Binds
    @Singleton
    abstract fun bindTenantRepository(
        tenantRepositoryImpl: TenantRepositoryImpl
    ): TenantRepository

    @Binds
    @Singleton
    abstract fun bindReceiptRepository(
        receiptRepositoryImpl: ReceiptRepositoryImpl
    ): ReceiptRepository

    @Binds
    @Singleton
    abstract fun bindMobileMoneyService(
        mockMobileMoneyService: MockMobileMoneyService
    ): MobileMoneyService

    @Binds
    @Singleton
    abstract fun bindComplaintRepository(
        complaintRepositoryImpl: ComplaintRepositoryImpl
    ): ComplaintRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    @Singleton
    abstract fun bindAuditRepository(
        auditRepositoryImpl: AuditRepositoryImpl
    ): AuditRepository
}
