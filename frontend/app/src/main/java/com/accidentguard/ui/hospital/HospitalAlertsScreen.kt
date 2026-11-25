package com.accidentguard.ui.hospital

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.accidentguard.data.model.EmergencyAlert
import com.accidentguard.ui.navigation.Screen
import com.accidentguard.ui.theme.*
import com.accidentguard.viewmodel.HospitalViewModel

@Composable
fun HospitalAlertsScreen(
    navController: NavController,
    viewModel: HospitalViewModel,
    alerts: List<EmergencyAlert>
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredAlerts = remember(alerts, selectedFilter) {
        when (selectedFilter) {
            "ACTIVE" -> alerts.filter { it.status.equals("PENDING", true) || it.status.equals("DISPATCHED", true) }
            "RESOLVED" -> alerts.filter { it.status.equals("ADMITTED", true) || it.status.equals("CANCELLED", true) }
            else -> alerts
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AlertsHeaderSection(
            totalCount = alerts.size,
            activeCount = alerts.count { it.status.equals("PENDING", true) || it.status.equals("DISPATCHED", true) }
        )

        FilterChipsRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        if (filteredAlerts.isEmpty()) {
            EmptyAlertsState(selectedFilter)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredAlerts, key = { it.id }) { alert ->
                    EmergencyAlertAuditCard(
                        alert = alert,
                        onClick = {
                            navController.navigate(Screen.HospitalAlertDetail.createRoute(alert.id))
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun AlertsHeaderSection(totalCount: Int, activeCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        Text(
            "Emergency Audit Log",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            "Tracking $totalCount total incidents with $activeCount active",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AlertSummaryStat(label = "Total", value = totalCount.toString(), icon = Icons.Default.History, modifier = Modifier.weight(1f))
            AlertSummaryStat(label = "Active", value = activeCount.toString(), icon = Icons.Default.NotificationImportant, color = DangerRed, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AlertSummaryStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = Color.White, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun FilterChipsRow(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == "ALL",
            onClick = { onFilterSelected("ALL") },
            label = { Text("All Logs") },
            leadingIcon = { if (selectedFilter == "ALL") Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
        )
        FilterChip(
            selected = selectedFilter == "ACTIVE",
            onClick = { onFilterSelected("ACTIVE") },
            label = { Text("Active") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DangerRed.copy(alpha = 0.1f), selectedLabelColor = DangerRed)
        )
        FilterChip(
            selected = selectedFilter == "RESOLVED",
            onClick = { onFilterSelected("RESOLVED") },
            label = { Text("Resolved") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SafeGreen.copy(alpha = 0.1f), selectedLabelColor = SafeGreen)
        )
    }
}

@Composable
fun EmergencyAlertAuditCard(alert: EmergencyAlert, onClick: () -> Unit) {
    val isPending = alert.status.equals("PENDING", true)
    val isDispatched = alert.status.equals("DISPATCHED", true)
    val isAdmitted = alert.status.equals("ADMITTED", true)
    val isCancelled = alert.status.equals("CANCELLED", true)

    val statusColor = when {
        isPending -> DangerRed
        isDispatched -> WarningOrange
        isAdmitted -> SafeGreen
        isCancelled -> TextSecondary
        else -> PrimaryBlue
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(statusColor, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "INCIDENT #${alert.id}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        alert.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                alert.damageSeverity,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, Modifier.size(14.dp), tint = TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${alert.victim.userName} • ${alert.victim.vehicleName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, Modifier.size(14.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${alert.dateStr} at ${alert.timeStr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                
                Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun EmptyAlertsState(filter: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                when(filter) {
                    "ACTIVE" -> Icons.Default.CheckCircleOutline
                    else -> Icons.Default.History
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextSecondary.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                when(filter) {
                    "ACTIVE" -> "No Active Emergencies"
                    "RESOLVED" -> "No Resolved Cases Found"
                    else -> "No Incident History"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Text(
                "Everything looks clear for now.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
