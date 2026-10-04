package com.j01000011.nyumba.data.repository

import com.j01000011.nyumba.domain.model.User
import com.j01000011.nyumba.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(
        fullName: String, 
        email: String, 
        phoneNumber: String, 
        password: String, 
        role: UserRole,
        identificationNumber: String = "",
        emergencyContactName: String = "",
        emergencyContactPhone: String = ""
    ): Result<User>
    suspend fun logout()
    suspend fun getProfile(uid: String): Result<User>
}
