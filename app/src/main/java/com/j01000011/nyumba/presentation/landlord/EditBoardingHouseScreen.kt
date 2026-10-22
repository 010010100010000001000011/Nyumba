package com.j01000011.nyumba.presentation.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.components.*

@Composable
fun EditBoardingHouseScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    viewModel: BoardingHouseSwitcherViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val boardingHouses by viewModel.boardingHouses.collectAsState()
    val house = boardingHouses.find { it.boardingHouseId == houseId }

    // This screen has its own ViewModel instance, so the house list must be loaded here.
    LaunchedEffect(user) {
        user?.let { viewModel.loadBoardingHouses(it.uid) }
    }

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var houseRules by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(house) {
        if (house != null && !loaded) {
            name = house.name
            address = house.address
            phone = house.phone
            description = house.description
            houseRules = house.houseRules
            loaded = true
        }
    }

    Scaffold(topBar = { NyumbaTopBar(title = "Edit house", subtitle = house?.name, onBack = onNavigateBack) }) { padding ->
        if (house == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader("Basics")
            NyumbaTextField(value = name, onValueChange = { name = it }, label = "House name", leadingIcon = Icons.Default.Apartment, isError = name.isBlank())
            NyumbaTextField(value = address, onValueChange = { address = it }, label = "Address / area", leadingIcon = Icons.Default.LocationOn)
            NyumbaTextField(value = phone, onValueChange = { phone = it }, label = "Contact phone", leadingIcon = Icons.Default.Phone, keyboardType = KeyboardType.Phone)

            SectionHeader("Shown to tenants")
            NyumbaTextField(
                value = description, onValueChange = { description = it },
                label = "Description", placeholder = "Location highlights, amenities, security...",
                singleLine = false, minLines = 3
            )
            NyumbaTextField(
                value = houseRules, onValueChange = { houseRules = it },
                label = "House rules", placeholder = "Visiting hours, quiet times, cooking, cleaning...",
                singleLine = false, minLines = 5
            )

            PrimaryButton(
                text = "Save changes",
                icon = Icons.Default.Save,
                enabled = name.isNotBlank(),
                onClick = {
                    viewModel.updateBoardingHouse(
                        house.copy(
                            name = name.trim(),
                            address = address.trim(),
                            phone = phone.trim(),
                            description = description.trim(),
                            houseRules = houseRules.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    onNavigateBack()
                }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
