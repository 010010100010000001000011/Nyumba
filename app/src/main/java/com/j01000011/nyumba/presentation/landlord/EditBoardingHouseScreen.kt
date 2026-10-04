package com.j01000011.nyumba.presentation.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBoardingHouseScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: BoardingHouseSwitcherViewModel = hiltViewModel()
) {
    val boardingHouses by viewModel.boardingHouses.collectAsState()
    val house = boardingHouses.find { it.boardingHouseId == houseId }

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var houseRules by remember { mutableStateOf("") }

    LaunchedEffect(house) {
        house?.let {
            name = it.name
            address = it.address
            phone = it.phone
            description = it.description
            houseRules = it.houseRules
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Boarding House") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (house == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("House Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = houseRules,
                    onValueChange = { houseRules = it },
                    label = { Text("House Rules") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )

                Button(
                    onClick = {
                        val updated = house.copy(
                            name = name,
                            address = address,
                            phone = phone,
                            description = description,
                            houseRules = houseRules,
                            updatedAt = System.currentTimeMillis()
                        )
                        viewModel.updateBoardingHouse(updated)
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}
