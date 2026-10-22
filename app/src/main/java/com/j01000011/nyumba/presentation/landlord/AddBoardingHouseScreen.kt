package com.j01000011.nyumba.presentation.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.components.*

@Composable
fun AddBoardingHouseScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    switcherViewModel: BoardingHouseSwitcherViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val user by authViewModel.currentUser.collectAsState()
    val createState by switcherViewModel.createState.collectAsState()

    LaunchedEffect(createState) {
        if (createState is CreateHouseState.Success) {
            switcherViewModel.clearCreateState()
            onNavigateBack()
        }
    }

    val save = {
        submitted = true
        val uid = user?.uid
        if (uid != null && name.isNotBlank() && address.isNotBlank() && phone.isNotBlank()) {
            switcherViewModel.createBoardingHouse(name.trim(), address.trim(), phone.trim(), uid)
        }
    }

    Scaffold(topBar = { NyumbaTopBar(title = "Add boarding house", onBack = onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NyumbaTextField(
                value = name, onValueChange = { name = it }, label = "House name",
                leadingIcon = Icons.Default.Apartment, placeholder = "e.g. Kabulonga Lodge",
                isError = submitted && name.isBlank(), supportingText = if (submitted && name.isBlank()) "Enter a name" else null
            )
            NyumbaTextField(
                value = address, onValueChange = { address = it }, label = "Address / area",
                leadingIcon = Icons.Default.LocationOn, placeholder = "Plot, street, area, town",
                isError = submitted && address.isBlank(), supportingText = if (submitted && address.isBlank()) "Enter the address tenants will see" else null
            )
            NyumbaTextField(
                value = phone, onValueChange = { phone = it }, label = "Contact phone",
                leadingIcon = Icons.Default.Phone, keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done, onImeAction = save,
                isError = submitted && phone.isBlank(), supportingText = if (submitted && phone.isBlank()) "Enter a contact number" else null
            )
            InlineMessage(
                text = "You can add a description and house rules after saving, from House details → Edit.",
                icon = Icons.Default.Info,
                tone = Tone.Info
            )
            (createState as? CreateHouseState.Error)?.let { InlineMessage(it.message, Icons.Default.ErrorOutline) }
            PrimaryButton(
                text = "Save boarding house",
                onClick = save,
                loading = createState is CreateHouseState.Loading,
                icon = Icons.Default.AddHome
            )
        }
    }
}
