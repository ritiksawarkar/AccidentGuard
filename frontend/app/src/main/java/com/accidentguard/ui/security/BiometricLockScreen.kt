package com.accidentguard.ui.security

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.accidentguard.data.local.TokenManager
import com.accidentguard.ui.theme.*

@Composable
fun BiometricLockScreen(
    onSuccess: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val biometricHelper = remember { BiometricHelper(context) }

    fun launchBiometric() {
        if (biometricHelper.isBiometricAvailable()) {
            biometricHelper.showBiometricPrompt(
                activity = context as FragmentActivity,
                title = "App Locked",
                subtitle = "Verify identity to continue",
                onSuccess = { onSuccess() },
                onError = { _, err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                },
                onFailed = { }
            )
        }
    }

    LaunchedEffect(Unit) {
        launchBiometric()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "App Locked",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please verify your identity to access safety features.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            IconButton(
                onClick = { launchBiometric() },
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Retry Biometric",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "Tap to unlock",
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            TextButton(
                onClick = onLogout,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = "Sign out from ${tokenManager.getUserName()}",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
