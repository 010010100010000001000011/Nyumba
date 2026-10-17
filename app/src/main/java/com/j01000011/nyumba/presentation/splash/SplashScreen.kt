package com.j01000011.nyumba.presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.UserRole
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLanding: () -> Unit,
    onNavigateToTenantDashboard: () -> Unit,
    onNavigateToLandlordDashboard: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser) {
        delay(1500) // Artificial delay for splash feel
        if (currentUser == null) {
            onNavigateToLanding()
        } else {
            when (currentUser?.role) {
                UserRole.TENANT -> onNavigateToTenantDashboard()
                UserRole.LANDLORD -> onNavigateToLandlordDashboard()
                else -> onNavigateToLanding()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nyumba",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}
