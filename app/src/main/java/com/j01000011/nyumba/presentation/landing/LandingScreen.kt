package com.j01000011.nyumba.presentation.landing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.j01000011.nyumba.R
import com.j01000011.nyumba.ui.theme.BrandGold
import com.j01000011.nyumba.ui.theme.HeroGradientStart

@Composable
fun LandingScreen(
    onNavigateToLandlord: () -> Unit,
    onNavigateToTenant: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(id = R.drawable.landingpage_background_image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.45f to HeroGradientStart.copy(alpha = 0.75f),
                        1f to Color(0xFF041F19)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Brand
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Apartment, contentDescription = null, tint = HeroGradientStart)
                }
                Spacer(Modifier.width(12.dp))
                Text("Nyumba", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.weight(1f).heightIn(min = 64.dp))

            // Hero
            Text(
                "Boarding houses,\nmanaged beautifully.",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Rooms, rent, mobile-money payments, receipts and tenant conversations — all in one place.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.82f)
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureChip(Icons.Default.Payments, "Mobile money")
                FeatureChip(Icons.AutoMirrored.Filled.ReceiptLong, "Receipts")
                FeatureChip(Icons.Default.Forum, "Chat")
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "Get started",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(10.dp))

            RoleCard(
                icon = Icons.Default.Key,
                title = "I'm a tenant",
                subtitle = "Find a room, pay rent and get receipts",
                onClick = onNavigateToTenant,
                highlighted = true
            )
            Spacer(Modifier.height(12.dp))
            RoleCard(
                icon = Icons.Default.HomeWork,
                title = "I'm a landlord",
                subtitle = "Manage houses, tenants and collections",
                onClick = onNavigateToLandlord,
                highlighted = false
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeatureChip(icon: ImageVector, label: String) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = BrandGold, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    highlighted: Boolean
) {
    val container = if (highlighted) BrandGold else Color.White.copy(alpha = 0.10f)
    val content = if (highlighted) Color(0xFF2C1800) else Color.White
    Surface(
        onClick = onClick,
        color = container,
        shape = MaterialTheme.shapes.large,
        border = if (highlighted) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(content.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = content)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = content)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.75f))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = content)
        }
    }
}
