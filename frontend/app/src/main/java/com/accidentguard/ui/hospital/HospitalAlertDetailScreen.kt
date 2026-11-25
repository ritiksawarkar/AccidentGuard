package com.accidentguard.ui.hospital

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.accidentguard.data.model.EmergencyAlert
import com.accidentguard.ui.theme.*
import com.accidentguard.viewmodel.HospitalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalAlertDetailScreen(
    navController: NavController,
    viewModel: HospitalViewModel,
    alertId: Long
) {
    val alerts by viewModel.alerts.collectAsState()
    val alert = remember(alerts, alertId) { alerts.find { it.id == alertId } }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (alert == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Banner
                StatusBanner(alert)

                // Victim Information
                DetailSection(title = "Victim & Vehicle", icon = Icons.Default.Person) {
                    InfoRow("Name", alert.victim.userName)
                    InfoRow("Phone", alert.victim.userPhone)
                    InfoRow("Vehicle", alert.victim.vehicleName)
                    InfoRow("Plate #", alert.victim.vehicleNumber)
                }

                // Accident Details
                DetailSection(title = "Accident Data", icon = Icons.Default.Warning) {
                    InfoRow("Severity", alert.damageSeverity)
                    InfoRow("Impact", "${alert.impactG} G")
                    InfoRow("Date", alert.dateStr)
                    InfoRow("Time", alert.timeStr)
                }

                // Dispatch Info (if any)
                if (alert.status != "PENDING") {
                    DetailSection(title = "Emergency Response", icon = Icons.Default.LocalShipping) {
                        InfoRow("Unit #", alert.ambulanceNumber ?: "N/A")
                        InfoRow("Driver", alert.driverName ?: "N/A")
                        InfoRow("ETA", "${alert.etaMinutes ?: 0} mins")
                        if (!alert.notes.isNullOrBlank()) {
                            InfoRow("Notes", alert.notes)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Button(
                    onClick = {
                        val uri = Uri.parse("geo:${alert.latitude},${alert.longitude}?q=${alert.latitude},${alert.longitude}(Accident+Location)")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View on Google Maps")
                }

                if (alert.status == "PENDING") {
                    Button(
                        onClick = { viewModel.directDispatchAmbulance(alert.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instant Dispatch")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBanner(alert: EmergencyAlert) {
    val statusColor = when (alert.status.uppercase()) {
        "PENDING" -> DangerRed
        "DISPATCHED" -> WarningOrange
        "ADMITTED" -> SafeGreen
        else -> TextSecondary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = statusColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "STATUS: ${alert.status.uppercase()}",
                fontWeight = FontWeight.Black,
                color = statusColor,
                letterSpacing = 1.sp
            )
            Text(
                "Incident ID: #${alert.id}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun DetailSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}
