package com.j01000011.nyumba.presentation.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.PaymentMethod
import com.j01000011.nyumba.presentation.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakePaymentScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    rentViewModel: RentViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val rentState by rentViewModel.rentState.collectAsState()

    var amountString by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(rentState) {
        if (rentState is RentUiState.Success) {
            rentViewModel.clearState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Make Payment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val state = rentState) {
                is RentUiState.AwaitingPayment -> {
                    Icon(
                        Icons.Default.CheckCircle, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp), 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Payment Initiated", style = MaterialTheme.typography.headlineSmall)
                    Text("Please confirm the prompt on your phone.")
                    Text("Transaction ID: ${state.transactionId}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                    Text("Waiting for confirmation...", style = MaterialTheme.typography.labelLarge)
                }
                else -> {
                    Text("Enter Payment Details", style = MaterialTheme.typography.titleLarge)

                    OutlinedTextField(
                        value = amountString,
                        onValueChange = { amountString = it },
                        label = { Text("Amount (K)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (rentState is RentUiState.Loading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                val amount = amountString.toDoubleOrNull() ?: 0.0
                                if (amount > 0 && user != null) {
                                    rentViewModel.makePayment(user!!, amount, PaymentMethod.MOBILE_MONEY, notes)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = amountString.isNotBlank()
                        ) {
                            Text("Pay via Mobile Money")
                        }
                    }

                    if (rentState is RentUiState.Error) {
                        Text(
                            text = (rentState as RentUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
