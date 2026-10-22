package com.j01000011.nyumba.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.UserRole
import com.j01000011.nyumba.ui.components.InlineMessage
import com.j01000011.nyumba.ui.components.NyumbaTextField
import com.j01000011.nyumba.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    initialRole: UserRole = UserRole.TENANT,
    onNavigateToLogin: (UserRole?) -> Unit,
    onRegisterSuccess: (UserRole) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var identificationNumber by remember { mutableStateOf("") }
    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactPhone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(initialRole) }
    var submitted by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onRegisterSuccess((authState as AuthState.Success).user.role)
            viewModel.clearState()
        }
    }

    val nameError = submitted && fullName.isBlank()
    val emailError = submitted && !isValidEmail(email)
    val phoneError = submitted && phoneNumber.filter { it.isDigit() }.length < 9
    val passwordError = submitted && password.length < 6

    val register = {
        submitted = true
        focusManager.clearFocus()
        val valid = fullName.isNotBlank() && isValidEmail(email) &&
            phoneNumber.filter { it.isDigit() }.length >= 9 && password.length >= 6
        if (valid) {
            viewModel.register(
                fullName = fullName.trim(),
                email = email.trim(),
                phoneNumber = phoneNumber.trim(),
                password = password,
                role = selectedRole,
                identificationNumber = identificationNumber.trim(),
                emergencyContactName = emergencyContactName.trim(),
                emergencyContactPhone = emergencyContactPhone.trim()
            )
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AuthHeader(
                title = "Create your account",
                subtitle = "It only takes a minute. You can update your details later."
            )
            Spacer(Modifier.height(4.dp))

            Text("I am a", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(UserRole.TENANT to "Tenant", UserRole.LANDLORD to "Landlord").forEachIndexed { index, (role, label) ->
                    SegmentedButton(
                        selected = selectedRole == role,
                        onClick = { selectedRole = role },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = selectedRole == role) {
                                Icon(
                                    if (role == UserRole.TENANT) Icons.Default.Key else Icons.Default.HomeWork,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                )
                            }
                        }
                    ) { Text(label) }
                }
            }

            NyumbaTextField(
                value = fullName, onValueChange = { fullName = it }, label = "Full name",
                leadingIcon = Icons.Default.Person,
                isError = nameError, supportingText = if (nameError) "Enter your name" else null
            )
            NyumbaTextField(
                value = email, onValueChange = { email = it }, label = "Email address",
                leadingIcon = Icons.Default.Mail, keyboardType = KeyboardType.Email,
                isError = emailError, supportingText = if (emailError) "Enter a valid email address" else null
            )
            NyumbaTextField(
                value = phoneNumber, onValueChange = { phoneNumber = it }, label = "Mobile number",
                leadingIcon = Icons.Default.Phone, keyboardType = KeyboardType.Phone, placeholder = "0977 123 456",
                isError = phoneError,
                supportingText = if (phoneError) "Enter a valid mobile number" else "Used for mobile-money payments"
            )
            NyumbaTextField(
                value = password, onValueChange = { password = it }, label = "Password",
                leadingIcon = Icons.Default.Lock, keyboardType = KeyboardType.Password, isPassword = true,
                imeAction = if (selectedRole == UserRole.TENANT) ImeAction.Next else ImeAction.Done,
                onImeAction = if (selectedRole == UserRole.TENANT) null else register,
                isError = passwordError, supportingText = if (passwordError) "Use at least 6 characters" else "At least 6 characters"
            )

            if (selectedRole == UserRole.TENANT) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Tenant details", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Optional now — your landlord may ask for these before approving a room.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NyumbaTextField(
                    value = identificationNumber, onValueChange = { identificationNumber = it },
                    label = "NRC / ID number", leadingIcon = Icons.Default.Badge
                )
                NyumbaTextField(
                    value = emergencyContactName, onValueChange = { emergencyContactName = it },
                    label = "Emergency contact name", leadingIcon = Icons.Default.ContactEmergency
                )
                NyumbaTextField(
                    value = emergencyContactPhone, onValueChange = { emergencyContactPhone = it },
                    label = "Emergency contact phone", leadingIcon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done, onImeAction = register
                )
            }

            (authState as? AuthState.Error)?.let {
                InlineMessage(text = friendlyAuthError(it.message), icon = Icons.Default.ErrorOutline)
            }

            PrimaryButton(text = "Create account", onClick = register, loading = authState is AuthState.Loading)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onNavigateToLogin(selectedRole) }) { Text("Sign in") }
            }
        }
    }
}
