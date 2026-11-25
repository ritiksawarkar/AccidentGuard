package com.accidentguard.ui.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.accidentguard.data.local.TokenManager
import com.accidentguard.ui.components.CustomOutlinedTextField
import com.accidentguard.ui.components.PrimaryButton
import com.accidentguard.ui.components.TextLink
import com.accidentguard.ui.security.BiometricHelper
import com.accidentguard.ui.theme.*
import com.accidentguard.viewmodel.AuthUiState
import com.accidentguard.viewmodel.AuthViewModel

enum class LoginRole {
    USER,
    HOSPITAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (role: String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val biometricHelper = remember { BiometricHelper(context) }

    var selectedRole by remember { mutableStateOf(LoginRole.USER) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var serverUrlInput by remember { mutableStateOf(tokenManager.getServerUrl()) }
    var showServerConfigDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                val successState = uiState as AuthUiState.Success
                Toast.makeText(context, successState.message, Toast.LENGTH_SHORT).show()
                val role = successState.role
                viewModel.resetState()
                onLoginSuccess(role)
            }
            is AuthUiState.Error -> {
                Toast.makeText(context, (uiState as AuthUiState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        if (tokenManager.isLoggedIn() && tokenManager.isBiometricEnabled() && biometricHelper.isBiometricAvailable()) {
            biometricHelper.showBiometricPrompt(
                activity = context as FragmentActivity,
                onSuccess = { onLoginSuccess(tokenManager.getUserRole()) },
                onError = { _, _ -> },
                onFailed = { }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.accidentguard.R.drawable.accident_guard_mark),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AccidentGuard",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        serverUrlInput = tokenManager.getServerUrl()
                        showServerConfigDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Server Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .windowInsetsPadding(WindowInsets.ime)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (selectedRole == LoginRole.USER) "Welcome back" else "Hospital Login",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (selectedRole == LoginRole.USER) 
                    "Sign in to manage your vehicle safety and emergency alerts." 
                else 
                    "Sign in to monitor emergency alerts and manage hospital response.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Account type",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Professional Segmented Control
            Surface(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder,
                color = Color.Transparent
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    val userSelected = selectedRole == LoginRole.USER
                    Button(
                        onClick = { selectedRole = LoginRole.USER },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (userSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (userSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = null,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vehicle User", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    val hospSelected = selectedRole == LoginRole.HOSPITAL
                    Button(
                        onClick = { selectedRole = LoginRole.HOSPITAL },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 0.dp, bottomStart = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hospSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                            contentColor = if (hospSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = null,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.LocalHospital, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hospital", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Email Field
            CustomOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = if (selectedRole == LoginRole.USER) "Email address" else "Hospital Email",
                leadingIcon = if (selectedRole == LoginRole.USER) Icons.Default.Mail else Icons.Default.Domain,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                tintColor = if (selectedRole == LoginRole.USER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Password Field
            CustomOutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                tintColor = if (selectedRole == LoginRole.USER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextLink(
                    text = "Forgot password?",
                    onClick = { /* Not implemented */ },
                    enabled = false,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sign In Button
            PrimaryButton(
                text = if (selectedRole == LoginRole.USER) "Sign In as Vehicle User" else "Sign In as Hospital",
                onClick = {
                    if (serverUrlInput.isNotBlank()) {
                        tokenManager.setServerUrl(serverUrlInput)
                    }
                    viewModel.login(email, password, if (selectedRole == LoginRole.USER) "user" else "hospital")
                },
                isLoading = isLoading,
                containerColor = if (selectedRole == LoginRole.USER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )

            if (biometricHelper.isBiometricAvailable() && tokenManager.isBiometricEnabled()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        biometricHelper.showBiometricPrompt(
                            activity = context as FragmentActivity,
                            onSuccess = { onLoginSuccess(tokenManager.getUserRole()) },
                            onError = { _, err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() },
                            onFailed = { }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sign in with Biometrics", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextLink(
                    text = if (selectedRole == LoginRole.USER) "Create Vehicle User Account" else "Register Your Hospital",
                    onClick = { onNavigateToRegister() },
                    color = if (selectedRole == LoginRole.USER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        if (showServerConfigDialog) {
            AlertDialog(
                onDismissRequest = { showServerConfigDialog = false },
                title = { Text("Server URL", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("Backend URL") },
                        placeholder = { Text("http://192.168.x.x:5000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        tokenManager.setServerUrl(serverUrlInput)
                        showServerConfigDialog = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showServerConfigDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
