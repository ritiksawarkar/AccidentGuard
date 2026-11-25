package com.accidentguard.ui.register

import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accidentguard.data.local.TokenManager
import com.accidentguard.ui.components.CustomOutlinedTextField
import com.accidentguard.ui.components.PrimaryButton
import com.accidentguard.ui.components.TextLink
import com.accidentguard.viewmodel.AuthUiState
import com.accidentguard.viewmodel.AuthViewModel

enum class RegistrationRole {
    USER,
    HOSPITAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember(context) { TokenManager(context) }

    val uiState by viewModel.uiState.collectAsState()

    var showServerConfigDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(tokenManager.getServerUrl()) }

    var selectedRole by remember { mutableStateOf(RegistrationRole.USER) }

    // User Form Fields
    var fullName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var userPassword by remember { mutableStateOf("") }
    var userConfirmPassword by remember { mutableStateOf("") }
    var userTermsAccepted by remember { mutableStateOf(false) }

    // Hospital Form Fields
    var hospitalName by remember { mutableStateOf("") }
    var hospitalEmail by remember { mutableStateOf("") }
    var hospitalPhone by remember { mutableStateOf("") }
    var hospitalCity by remember { mutableStateOf("Nagpur") }
    var hospitalAddress by remember { mutableStateOf("") }
    var hospitalAmbulances by remember { mutableStateOf("5") }
    var hospitalPassword by remember { mutableStateOf("") }
    var hospitalConfirmPassword by remember { mutableStateOf("") }
    var hospitalTermsAccepted by remember { mutableStateOf(false) }

    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is AuthUiState.Success) {
            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            viewModel.resetState()
            onRegisterSuccess()
        } else if (state is AuthUiState.Error) {
            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            viewModel.resetState()
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
                            contentDescription = "Settings",
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
                text = if (selectedRole == RegistrationRole.USER) "Create account" else "Hospital registration",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (selectedRole == RegistrationRole.USER) 
                    "Join the network for personal vehicle safety and emergency alerts." 
                else 
                    "Partner with us to provide rapid emergency response and medical care.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Registration type",
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
                    val userSelected = selectedRole == RegistrationRole.USER
                    Button(
                        onClick = { selectedRole = RegistrationRole.USER },
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

                    val hospSelected = selectedRole == RegistrationRole.HOSPITAL
                    Button(
                        onClick = { selectedRole = RegistrationRole.HOSPITAL },
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

            if (selectedRole == RegistrationRole.USER) {
                // User Form
                CustomOutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Full name",
                    leadingIcon = Icons.Default.Person,
                    tintColor = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomOutlinedTextField(
                    value = userEmail,
                    onValueChange = { userEmail = it },
                    label = "Email address",
                    leadingIcon = Icons.Default.Mail,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    tintColor = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomOutlinedTextField(
                    value = userPhone,
                    onValueChange = { userPhone = it },
                    label = "Phone number",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    tintColor = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomOutlinedTextField(
                    value = userPassword,
                    onValueChange = { userPassword = it },
                    label = "Password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    tintColor = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomOutlinedTextField(
                    value = userConfirmPassword,
                    onValueChange = { userConfirmPassword = it },
                    label = "Confirm password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    tintColor = MaterialTheme.colorScheme.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Checkbox(
                        checked = userTermsAccepted,
                        onCheckedChange = { userTermsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "I accept the Terms & Conditions",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                PrimaryButton(
                    text = "Create Account",
                    onClick = { viewModel.registerUser(fullName, userEmail, userPhone, userPassword, userConfirmPassword, userTermsAccepted) },
                    isLoading = isLoading,
                    containerColor = MaterialTheme.colorScheme.primary
                )
            } else {
                // Hospital Form
                CustomOutlinedTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = "Hospital name",
                    leadingIcon = Icons.Default.Domain,
                    tintColor = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomOutlinedTextField(
                    value = hospitalEmail,
                    onValueChange = { hospitalEmail = it },
                    label = "Official email",
                    leadingIcon = Icons.Default.Mail,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    tintColor = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomOutlinedTextField(
                    value = hospitalPhone,
                    onValueChange = { hospitalPhone = it },
                    label = "Emergency contact",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    tintColor = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = hospitalCity,
                        onValueChange = { hospitalCity = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                    OutlinedTextField(
                        value = hospitalAmbulances,
                        onValueChange = { hospitalAmbulances = it },
                        label = { Text("Ambulances") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                CustomOutlinedTextField(
                    value = hospitalAddress,
                    onValueChange = { hospitalAddress = it },
                    label = "Full address",
                    leadingIcon = Icons.Default.LocationOn,
                    tintColor = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomOutlinedTextField(
                    value = hospitalPassword,
                    onValueChange = { hospitalPassword = it },
                    label = "Password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    tintColor = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomOutlinedTextField(
                    value = hospitalConfirmPassword,
                    onValueChange = { hospitalConfirmPassword = it },
                    label = "Confirm password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    tintColor = MaterialTheme.colorScheme.secondary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Checkbox(
                        checked = hospitalTermsAccepted,
                        onCheckedChange = { hospitalTermsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                    )
                    Text(
                        text = "I accept the Dispatch Agreement",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                PrimaryButton(
                    text = "Register Hospital",
                    onClick = {
                        val ambNum = hospitalAmbulances.toIntOrNull() ?: 5
                        viewModel.registerHospital(hospitalName, hospitalEmail, hospitalPhone, hospitalAddress, hospitalCity, ambNum, hospitalPassword, hospitalConfirmPassword, hospitalTermsAccepted)
                    },
                    isLoading = isLoading,
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextLink(
                    text = "Sign In",
                    onClick = { onNavigateToLogin() },
                    color = if (selectedRole == RegistrationRole.USER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
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
                        shape = RoundedCornerShape(12.dp)
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
