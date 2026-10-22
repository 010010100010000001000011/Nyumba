package com.j01000011.nyumba.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.UserRole
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.theme.BrandGold
import com.j01000011.nyumba.ui.theme.HeroGradientEnd
import com.j01000011.nyumba.ui.theme.HeroGradientStart
import kotlinx.coroutines.delay

private const val MIN_SPLASH_MS = 700L

@Composable
fun SplashScreen(
    onNavigateToLanding: () -> Unit,
    onNavigateToTenantDashboard: () -> Unit,
    onNavigateToLandlordDashboard: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val authResolved by viewModel.authResolved.collectAsState()
    val shownAt = remember { System.currentTimeMillis() }

    // Wait until auth + profile have actually loaded, instead of guessing with a fixed delay.
    LaunchedEffect(authResolved) {
        if (!authResolved) return@LaunchedEffect
        delay(maxOf(0L, MIN_SPLASH_MS - (System.currentTimeMillis() - shownAt)))
        when (currentUser?.role) {
            UserRole.TENANT -> onNavigateToTenantDashboard()
            UserRole.LANDLORD -> onNavigateToLandlordDashboard()
            null -> onNavigateToLanding()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HeroGradientStart, HeroGradientEnd))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(BrandGold),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Apartment, contentDescription = null, tint = HeroGradientStart, modifier = Modifier.size(46.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Nyumba", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text("Boarding houses, managed beautifully", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(120.dp),
                color = BrandGold,
                trackColor = Color.White.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}
