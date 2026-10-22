package com.j01000011.nyumba.presentation.payments

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.RentStatus
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MakePaymentScreen(
    onNavigateBack: () -> Unit,
    onViewReceipt: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    rentViewModel: RentViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val rentState by rentViewModel.rentState.collectAsState()
    val periods by rentViewModel.rentPeriods.collectAsState()
    val focusManager = LocalFocusManager.current

    var amountText by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let {
            rentViewModel.loadTenantRentData(it.uid)
            if (phone.isBlank()) phone = it.phoneNumber
        }
    }

    val outstanding = periods
        .filter { it.isCurrentOrPastDue && it.status != RentStatus.PAID && it.status != RentStatus.ADVANCE_PAID }
        .sumOf { it.remainingBalance }
    val monthlyRent = user?.monthlyRent ?: 0.0

    val amount = amountText.toDoubleOrNull()
    val network = detectNetwork(phone)
    val amountError = submitted && (amount == null || amount <= 0)
    val phoneError = submitted && network == null

    val pay = {
        submitted = true
        focusManager.clearFocus()
        if (amount != null && amount > 0 && detectNetwork(phone) != null) {
            rentViewModel.makePayment(phone, amount, notes.ifBlank { null })
        }
    }

    Scaffold(
        topBar = {
            NyumbaTopBar(
                title = "Pay rent",
                subtitle = "Mobile money via Lipila",
                onBack = if (rentState is RentUiState.AwaitingPayment) null else onNavigateBack
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = when (rentState) {
                is RentUiState.AwaitingPayment -> 1
                is RentUiState.PaymentConfirmed -> 2
                else -> 0
            },
            label = "payment-step",
            modifier = Modifier.padding(padding)
        ) { step ->
            when (step) {
                1 -> (rentState as? RentUiState.AwaitingPayment)?.let { AwaitingApproval(it) }
                2 -> (rentState as? RentUiState.PaymentConfirmed)?.let { confirmed ->
                    PaymentSuccess(
                        amount = confirmed.amount,
                        onViewReceipt = confirmed.receiptId?.let { id -> { rentViewModel.clearState(); onViewReceipt(id) } },
                        onDone = { rentViewModel.clearState(); onNavigateBack() }
                    )
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeroCard {
                        Text("Outstanding balance", style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.8f))
                        Spacer(Modifier.height(4.dp))
                        Text(formatKwacha(outstanding), style = MaterialTheme.typography.headlineLarge)
                        if (monthlyRent > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text("Monthly rent ${formatKwacha(monthlyRent)}", style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.8f))
                        }
                    }

                    SectionHeader("Amount")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (outstanding > 0) {
                            SuggestionChip(onClick = { amountText = plain(outstanding) }, label = { Text("Full balance · ${formatKwacha(outstanding)}") })
                        }
                        if (monthlyRent > 0 && monthlyRent != outstanding) {
                            SuggestionChip(onClick = { amountText = plain(monthlyRent) }, label = { Text("One month · ${formatKwacha(monthlyRent)}") })
                        }
                    }
                    NyumbaTextField(
                        value = amountText,
                        onValueChange = { new -> if (new.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?$"))) amountText = new },
                        label = "Amount",
                        prefix = "K ",
                        keyboardType = KeyboardType.Decimal,
                        isError = amountError,
                        supportingText = if (amountError) "Enter an amount greater than zero" else null
                    )

                    SectionHeader("Pay from")
                    NyumbaTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Mobile money number",
                        leadingIcon = Icons.Default.PhoneAndroid,
                        placeholder = "0977 123 456",
                        keyboardType = KeyboardType.Phone,
                        isError = phoneError,
                        supportingText = when {
                            phoneError -> "Enter an MTN, Airtel or Zamtel number"
                            network != null -> "$network Mobile Money"
                            else -> "MTN, Airtel or Zamtel"
                        }
                    )
                    NyumbaTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 120) notes = it },
                        label = "Note (optional)",
                        leadingIcon = Icons.AutoMirrored.Filled.Notes,
                        imeAction = ImeAction.Done,
                        onImeAction = pay
                    )

                    (rentState as? RentUiState.Error)?.let {
                        InlineMessage(text = it.message, icon = Icons.Default.ErrorOutline)
                    }

                    PrimaryButton(
                        text = if (amount != null && amount > 0) "Pay ${formatKwacha(amount)}" else "Pay",
                        onClick = pay,
                        loading = rentState is RentUiState.Loading,
                        icon = Icons.Default.Lock
                    )
                    Text(
                        "You'll get a prompt on your phone to approve with your mobile-money PIN. Your receipt is generated automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AwaitingApproval(state: RentUiState.AwaitingPayment) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(96.dp), strokeWidth = 4.dp)
            Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(28.dp))
        Text("Check your phone", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Approve the ${formatKwacha(state.amount)} request sent to ${state.phoneNumber} by entering your mobile-money PIN.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        SurfaceCard {
            InfoRow("Amount", formatKwacha(state.amount))
            InfoRow("Reference", state.transactionId)
            InfoRow("Status", "Waiting for approval")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Keep this screen open. It updates automatically once the payment is confirmed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PaymentSuccess(amount: Double, onViewReceipt: (() -> Unit)?, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconBadge(Icons.Default.CheckCircle, tone = Tone.Success, size = 88)
        Spacer(Modifier.height(24.dp))
        Text("Payment received", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(formatKwacha(amount), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your rent has been updated and a receipt has been generated.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        if (onViewReceipt != null) {
            PrimaryButton(text = "View receipt", onClick = onViewReceipt, icon = Icons.AutoMirrored.Filled.ReceiptLong)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.medium) {
            Text("Done")
        }
    }
}

/** Identifies the Zambian mobile network from the number prefix, or null if it isn't a valid mobile. */
internal fun detectNetwork(phone: String): String? {
    val digits = phone.filter { it.isDigit() }
    val local = when {
        digits.startsWith("260") && digits.length == 12 -> "0" + digits.drop(3)
        digits.length == 9 -> "0$digits"
        else -> digits
    }
    if (local.length != 10) return null
    return when (local.take(3)) {
        "096", "076" -> "MTN"
        "097", "077" -> "Airtel"
        "095", "075" -> "Zamtel"
        else -> null
    }
}

private fun plain(amount: Double): String = "%.2f".format(java.util.Locale.US, amount)
