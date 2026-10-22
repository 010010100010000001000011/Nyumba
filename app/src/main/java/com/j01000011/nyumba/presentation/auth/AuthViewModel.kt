package com.j01000011.nyumba.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.data.repository.AuthRepository
import com.j01000011.nyumba.domain.model.User
import com.j01000011.nyumba.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    /** False until the signed-in state (and profile, if signed in) has been loaded once. */
    private val _authResolved = MutableStateFlow(false)
    val authResolved: StateFlow<Boolean> = _authResolved.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    val profileResult = authRepository.getProfile(user.uid)
                    _currentUser.value = profileResult.getOrNull() ?: user
                } else {
                    _currentUser.value = null
                }
                _authResolved.value = true
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.login(email, password)
                .onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                }
                .onFailure { e ->
                    _authState.value = AuthState.Error(e.message ?: "Login failed")
                }
        }
    }

    fun register(
        fullName: String, 
        email: String, 
        phoneNumber: String, 
        password: String, 
        role: UserRole,
        identificationNumber: String = "",
        emergencyContactName: String = "",
        emergencyContactPhone: String = ""
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.register(
                fullName, 
                email, 
                phoneNumber, 
                password, 
                role,
                identificationNumber,
                emergencyContactName,
                emergencyContactPhone
            )
                .onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                }
                .onFailure { e ->
                    _authState.value = AuthState.Error(e.message ?: "Registration failed")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (_: Exception) {
                // Ignore exception to guarantee logout state reset
            } finally {
                _currentUser.value = null
                _authState.value = AuthState.Idle
            }
        }
    }

    /** Sends a Firebase password-reset email; [onResult] receives null on success or an error message. */
    fun sendPasswordReset(email: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            authRepository.sendPasswordReset(email.trim())
                .onSuccess { onResult(null) }
                .onFailure { onResult(it.message ?: "Could not send reset email") }
        }
    }

    fun clearState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
