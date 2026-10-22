package com.j01000011.nyumba.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailRead
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
import com.j01000011.nyumba.ui.components.Tone

@Composable
fun LoginScreen(
    initialRole: UserRole? = null,
    onNavigateToRegister: (UserRole?) -> Unit,
    onLoginSuccess: (UserRole) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // text, isError
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess((authState as AuthState.Success).user.role)
            viewModel.clearState()
        }
    }

    val emailError = submitted && !isValidEmail(email)
    val passwordError = submitted && password.isBlank()

    val signIn = {
        submitted = true
        focusManager.clearFocus()
        if (isValidEmail(email) && password.isNotBlank()) viewModel.login(email.trim(), password)
    }

    val subtitle = when (initialRole) {
        UserRole.LANDLORD -> "Sign in to manage your boarding houses."
        UserRole.TENANT -> "Sign in to see your room, rent and receipts."
        null -> "Sign in to continue to Nyumba."
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthHeader(title = "Welcome back", subtitle = subtitle)
            Spacer(Modifier.height(8.dp))

            NyumbaTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email address",
                leadingIcon = Icons.Default.Mail,
                keyboardType = KeyboardType.Email,
                isError = emailError,
                supportingText = if (emailError) "Enter a valid email address" else null
            )
            NyumbaTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = signIn,
                isError = passwordError,
                supportingText = if (passwordError) "Enter your password" else null
            )

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = {
                    if (!isValidEmail(email)) {
                        resetMessage = "Enter your email above, then tap Forgot password." to true
                    } else {
                        viewModel.sendPasswordReset(email) { error ->
                            resetMessage = (error ?: "Password reset link sent to ${email.trim()}.") to (error != null)
                        }
                    }
                }) { Text("Forgot password?") }
            }

            resetMessage?.let { (text, isError) ->
                InlineMessage(
                    text = text,
                    icon = if (isError) Icons.Default.ErrorOutline else Icons.Default.MarkEmailRead,
                    tone = if (isError) Tone.Warning else Tone.Success
                )
            }

            (authState as? AuthState.Error)?.let {
                InlineMessage(text = friendlyAuthError(it.message), icon = Icons.Default.ErrorOutline)
            }

            PrimaryButton(
                text = "Sign in",
                onClick = signIn,
                loading = authState is AuthState.Loading,
                icon = Icons.AutoMirrored.Filled.Login
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New to Nyumba?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onNavigateToRegister(initialRole) }) { Text("Create an account") }
            }
        }
    }
}

/** Firebase Auth messages are technical; map the common ones to plain language. */
internal fun friendlyAuthError(message: String): String = when {
    message.contains("password is invalid", true) ||
        message.contains("credential is incorrect", true) ||
        message.contains("INVALID_LOGIN_CREDENTIALS", true) -> "Incorrect email or password."
    message.contains("no user record", true) -> "No account found with that email."
    message.contains("already in use", true) -> "An account with this email already exists. Try signing in."
    message.contains("network", true) -> "No internet connection. Check your network and try again."
    message.contains("blocked", true) || message.contains("too many", true) -> "Too many attempts. Please wait a moment and try again."
    message.contains("at least 6", true) || message.contains("WEAK_PASSWORD", true) -> "Password must be at least 6 characters."
    else -> message
}
