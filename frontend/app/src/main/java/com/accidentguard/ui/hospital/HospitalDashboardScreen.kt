package com.accidentguard.ui.hospital

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.accidentguard.data.local.TokenManager
import com.accidentguard.data.model.EmergencyAlert
import com.accidentguard.data.model.HospitalStats
import com.accidentguard.ui.navigation.Screen
import com.accidentguard.ui.theme.*
import com.accidentguard.viewmodel.HospitalUiState
import com.accidentguard.viewmodel.HospitalViewModel

enum class HospitalTab {
    HOME, ALERTS, AMBULANCE, PATIENTS, PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalDashboardScreen(
    navController: NavController,
    viewModel: HospitalViewModel
) {
    val context = LocalContext.current
    val alerts by viewModel.alerts.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var currentTab by remember { mutableStateOf(HospitalTab.HOME) }
    var showCustomDispatchDialog by remember { mutableStateOf<EmergencyAlert?>(null) }
    var showFleetManageDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val isActionLoading = uiState is HospitalUiState.ActionLoading

    LaunchedEffect(uiState) {
        when (uiState) {
            is HospitalUiState.Success -> {
                Toast.makeText(context, (uiState as HospitalUiState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            is HospitalUiState.Error -> {
                Toast.makeText(context, (uiState as HospitalUiState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            HospitalTopAppBar(
                hospitalName = viewModel.tokenManager.getUserName().ifBlank { "Emergency Ops Center" },
                hospitalCity = viewModel.tokenManager.getHospitalCity(),
                isRefreshing = uiState is HospitalUiState.Loading,
                currentTab = currentTab,
                onRefresh = { viewModel.loadDashboard(isUserInitiated = true) },
                onLogout = { showLogoutDialog = true }
            )
        },
        bottomBar = {
            HospitalBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                
                Crossfade(targetState = currentTab, label = "TabTransition") { tab ->
                    when (tab) {
                        HospitalTab.HOME -> HomeDashboardContent(
                            stats = stats,
                            alerts = alerts,
                            onManageFleet = { showFleetManageDialog = true },
                            onCallVictim = { phone -> callPhone(context, phone) },
                            onOpenMap = { lat, lng -> openMap(context, lat, lng) },
                            onDirectDispatch = { id -> viewModel.directDispatchAmbulance(id) },
                            onCustomDispatch = { alert -> showCustomDispatchDialog = alert },
                            onAdmit = { id -> viewModel.admitPatient(id) },
                            onViewAll = { currentTab = HospitalTab.ALERTS },
                            isLoading = uiState is HospitalUiState.Loading
                        )
                        HospitalTab.ALERTS -> HospitalAlertsScreen(navController, viewModel, alerts)
                        HospitalTab.AMBULANCE -> FleetContent(stats, onManageFleet = { showFleetManageDialog = false })
                        HospitalTab.PATIENTS -> PatientsContent(stats)
                        HospitalTab.PROFILE -> ProfileContent(viewModel.tokenManager)
                    }
                }
            }

            if (isActionLoading) {
                ActionLoadingOverlay()
            }

            showCustomDispatchDialog?.let { alert ->
                CustomDispatchAmbulanceDialog(
                    alert = alert,
                    onDismiss = { showCustomDispatchDialog = null },
                    onConfirm = { ambNo, driverName, driverPhone, eta, notes ->
                        viewModel.dispatchAmbulance(alert.id, ambNo, driverName, driverPhone, eta, notes)
                        showCustomDispatchDialog = null
                    }
                )
            }

            if (showFleetManageDialog) {
                FleetManageDialog(
                    stats = stats,
                    onDismiss = { showFleetManageDialog = false },
                    onUpdate = { total, avail ->
                        viewModel.updateFleet(total = total, available = avail)
                        showFleetManageDialog = false
                    }
                )
            }

            if (showLogoutDialog) {
                LogoutConfirmationDialog(
                    onConfirm = {
                        viewModel.logout()
                        showLogoutDialog = false
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onDismiss = { showLogoutDialog = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalTopAppBar(
    hospitalName: String,
    hospitalCity: String,
    isRefreshing: Boolean,
    currentTab: HospitalTab,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    TopAppBar(
        title = {
            if (currentTab == HospitalTab.ALERTS) {
                Text(
                    text = "Emergency Alerts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Column {
                    Text(
                        text = hospitalName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalHospital, 
                            contentDescription = null, 
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$hospitalCity • Status: Online",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
    )
}

@Composable
fun HospitalBottomNavigation(
    currentTab: HospitalTab,
    onTabSelected: (HospitalTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(HospitalTab.HOME, "Home", Icons.Default.Home),
            Triple(HospitalTab.ALERTS, "Alerts", Icons.Default.Notifications),
            Triple(HospitalTab.AMBULANCE, "Fleet", Icons.Default.LocalShipping),
            Triple(HospitalTab.PATIENTS, "Patients", Icons.Default.People),
            Triple(HospitalTab.PROFILE, "Profile", Icons.Default.Person)
        )

        items.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun HomeDashboardContent(
    stats: HospitalStats?,
    alerts: List<EmergencyAlert>,
    onManageFleet: () -> Unit,
    onCallVictim: (String) -> Unit,
    onOpenMap: (Double, Double) -> Unit,
    onDirectDispatch: (Long) -> Unit,
    onCustomDispatch: (EmergencyAlert) -> Unit,
    onAdmit: (Long) -> Unit,
    onViewAll: () -> Unit,
    isLoading: Boolean
) {
    val activeAlerts = alerts.filter { it.status.equals("PENDING", true) || it.status.equals("DISPATCHED", true) }
    val historicalAlerts = alerts.filter { it.status.equals("ADMITTED", true) || it.status.equals("CANCELLED", true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HospitalSummaryBanner(stats, onManageFleet)
        }

        item {
            QuickActionsGrid(
                onDispatch = { 
                    activeAlerts.firstOrNull { it.status == "PENDING" }?.let { onDirectDispatch(it.id) }
                },
                onAlerts = onViewAll,
                onFleet = onManageFleet,
                onPatients = { /* Navigate or show stats */ }
            )
        }

        // --- ACTIVE INCIDENTS SECTION ---
        item {
            SectionHeader(
                title = "Active Emergencies",
                count = activeAlerts.size,
                countLabel = "Active",
                accentColor = DangerRed
            )
        }

        if (activeAlerts.isEmpty() && !isLoading) {
            item { EmptyActiveIncidentsState() }
        } else {
            items(activeAlerts, key = { "active_${it.id}" }) { alert ->
                HospitalAlertCard(
                    alert = alert,
                    onCallVictim = onCallVictim,
                    onOpenMap = onOpenMap,
                    onDirectDispatch = { onDirectDispatch(alert.id) },
                    onCustomDispatch = { onCustomDispatch(alert) },
                    onAdmit = { onAdmit(alert.id) }
                )
            }
        }

        // --- RECENT HISTORY SECTION ---
        if (historicalAlerts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Recent History",
                        count = historicalAlerts.size,
                        countLabel = "Past",
                        accentColor = TextSecondary
                    )
                    TextButton(
                        onClick = onViewAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("View All", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            items(historicalAlerts.take(5), key = { "history_${it.id}" }) { alert ->
                HospitalAlertCard(
                    alert = alert,
                    onCallVictim = onCallVictim,
                    onOpenMap = onOpenMap,
                    onDirectDispatch = { },
                    onCustomDispatch = { },
                    onAdmit = { }
                )
            }
        } else if (historicalAlerts.isEmpty() && !isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("No incident history available", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun SectionHeader(title: String, count: Int, countLabel: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (count > 0) {
            Surface(
                color = accentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "$count $countLabel",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HospitalSummaryBanner(
    stats: HospitalStats?,
    onManageFleet: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FLEET STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${stats?.availableAmbulances ?: 0} Units Ready",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                        .clickable { onManageFleet() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatItem(label = "Pending", value = stats?.pendingEmergencies ?: 0, color = DangerRed, modifier = Modifier.weight(1f))
                StatItem(label = "En Route", value = stats?.activeDispatches ?: 0, color = WarningOrange, modifier = Modifier.weight(1f))
                StatItem(label = "Admitted", value = stats?.admittedPatients ?: 0, color = SafeGreen, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
    }
}

@Composable
fun QuickActionsGrid(
    onDispatch: () -> Unit,
    onAlerts: () -> Unit,
    onFleet: () -> Unit,
    onPatients: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionItem("Dispatch", Icons.Default.FlashOn, DangerRed, onDispatch, Modifier.weight(1f))
        QuickActionItem("Alerts", Icons.Default.Warning, WarningOrange, onAlerts, Modifier.weight(1f))
        QuickActionItem("Fleet", Icons.Default.LocalShipping, PrimaryBlue, onFleet, Modifier.weight(1f))
        QuickActionItem("Patients", Icons.Default.History, SafeGreen, onPatients, Modifier.weight(1f))
    }
}

@Composable
fun QuickActionItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
fun HospitalAlertCard(
    alert: EmergencyAlert,
    onCallVictim: (String) -> Unit,
    onOpenMap: (Double, Double) -> Unit,
    onDirectDispatch: () -> Unit,
    onCustomDispatch: () -> Unit,
    onAdmit: () -> Unit
) {
    val isPending = alert.status.equals("PENDING", ignoreCase = true)
    val isDispatched = alert.status.equals("DISPATCHED", ignoreCase = true)
    
    val statusColor = when {
        isPending -> DangerRed
        isDispatched -> WarningOrange
        else -> SafeGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, if (isPending) DangerRed.copy(alpha = 0.2f) else Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPending) "PRIORITY ALERT" else alert.status.uppercase(),
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Text(
                    text = alert.timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = alert.damageSeverity,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isPending) DangerRed else MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(modifier = Modifier.fillMaxWidth()) {
                InfoBlock(
                    label = "VICTIM",
                    value = alert.victim.userName,
                    subValue = alert.victim.userPhone,
                    onSubClick = { onCallVictim(alert.victim.userPhone) },
                    modifier = Modifier.weight(1f)
                )
                InfoBlock(
                    label = "VEHICLE",
                    value = alert.victim.vehicleName,
                    subValue = alert.victim.vehicleNumber,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isDispatched && !alert.ambulanceNumber.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AmbulanceStatusStrip(alert)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenMap(alert.latitude, alert.longitude) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Live Map", fontSize = 12.sp)
                }

                if (isPending) {
                    Button(
                        onClick = onDirectDispatch,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DISPATCH NOW", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else if (isDispatched) {
                    Button(
                        onClick = onAdmit,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                    ) {
                        Text("MARK ADMITTED", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            
            if (isPending) {
                TextButton(
                    onClick = onCustomDispatch,
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                ) {
                    Text("Assign Specific Unit (Advanced)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun InfoBlock(label: String, value: String, subValue: String, onSubClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(
            text = subValue,
            style = MaterialTheme.typography.labelSmall,
            color = if (onSubClick != null) MaterialTheme.colorScheme.primary else TextSecondary,
            modifier = if (onSubClick != null) Modifier.clickable { onSubClick() } else Modifier
        )
    }
}

@Composable
fun AmbulanceStatusStrip(alert: EmergencyAlert) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = WarningOrange.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    "Unit ${alert.ambulanceNumber} • ETA ${alert.etaMinutes ?: 8}m",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = WarningOrange
                )
                Text(
                    "${alert.driverName ?: "Emergency Unit"} (${alert.driverPhone ?: "108"})",
                    fontSize = 10.sp,
                    color = WarningOrange.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun EmptyActiveIncidentsState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(50.dp),
            shape = CircleShape,
            color = SafeGreen.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("No Active Incidents", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("System in standby. Monitoring for emergencies.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun AlertsContent(alerts: List<EmergencyAlert>) {
    val historicalAlerts = alerts.filter { it.status.equals("ADMITTED", true) || it.status.equals("CANCELLED", true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    "Emergency Audit Log",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Comprehensive record of all historical incidents",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        if (historicalAlerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = TextSecondary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No incident history available", color = TextSecondary)
                    }
                }
            }
        } else {
            items(historicalAlerts, key = { "full_${it.id}" }) { alert ->
                val isAdmitted = alert.status.equals("ADMITTED", true)
                val isCancelled = alert.status.equals("CANCELLED", true)
                val statusColor = when {
                    isCancelled -> TextSecondary
                    isAdmitted -> SafeGreen
                    else -> WarningOrange
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "INCIDENT #${alert.id}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = statusColor.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    alert.status.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = statusColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            alert.damageSeverity,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Victim: ${alert.victim.userName} (${alert.victim.userPhone})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!alert.ambulanceNumber.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Ambulance: ${alert.ambulanceNumber} | Driver: ${alert.driverName ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${alert.dateStr} at ${alert.timeStr}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            
                            if (!alert.notes.isNullOrBlank()) {
                                Text(
                                    "Note: ${alert.notes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun FleetContent(stats: HospitalStats?, onManageFleet: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ambulance Fleet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(24.dp))
        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(80.dp), tint = PrimaryBlue.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("${stats?.totalAmbulances ?: 0} Total Units", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${stats?.availableAmbulances ?: 0} Currently Available", color = SafeGreen, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onManageFleet, modifier = Modifier.fillMaxWidth()) {
            Text("Update Fleet Availability")
        }
    }
}

@Composable
fun PatientsContent(stats: HospitalStats?) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Admitted Patients", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(24.dp))
        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(80.dp), tint = SafeGreen.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("${stats?.admittedPatients ?: 0} Active Admissions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Recently admitted via AccidentGuard", color = TextSecondary)
    }
}

@Composable
fun ProfileContent(tokenManager: TokenManager) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) {
                Text(tokenManager.getUserName().take(1), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(tokenManager.getUserName(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(tokenManager.getUserEmail(), color = TextSecondary)
        Spacer(modifier = Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileInfoRow("City", tokenManager.getHospitalCity())
                ProfileInfoRow("Address", tokenManager.getHospitalAddress())
                ProfileInfoRow("Role", "Hospital Operations")
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

// ── Helpers ──

private fun callPhone(context: android.content.Context, phone: String) {
    if (phone.isNotBlank() && phone != "N/A") {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (e: Exception) { }
    }
}

private fun openMap(context: android.content.Context, lat: Double, lng: Double) {
    if (lat != 0.0 && lng != 0.0) {
        try {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Accident+Location)")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) { }
    }
}

@Composable
private fun CustomDispatchAmbulanceDialog(
    alert: EmergencyAlert,
    onDismiss: () -> Unit,
    onConfirm: (ambNo: String, driverName: String, driverPhone: String, eta: Int, notes: String) -> Unit
) {
    var ambulanceNo by remember { mutableStateOf("AMB-${(alert.id % 15) + 1}") }
    var driverName by remember { mutableStateOf("Duty Driver") }
    var driverPhone by remember { mutableStateOf("108") }
    var etaMinutes by remember { mutableStateOf("8") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Dispatch", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = ambulanceNo, onValueChange = { ambulanceNo = it }, label = { Text("Ambulance #") }, singleLine = true)
                OutlinedTextField(value = driverName, onValueChange = { driverName = it }, label = { Text("Driver Name") }, singleLine = true)
                OutlinedTextField(
                    value = driverPhone,
                    onValueChange = { if (it.all { char -> char.isDigit() }) driverPhone = it },
                    label = { Text("Driver Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = etaMinutes,
                    onValueChange = { if (it.all { char -> char.isDigit() }) etaMinutes = it },
                    label = { Text("ETA (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(ambulanceNo, driverName, driverPhone, etaMinutes.toIntOrNull() ?: 8, "Manual dispatch") }) {
                Text("Dispatch")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FleetManageDialog(
    stats: HospitalStats?,
    onDismiss: () -> Unit,
    onUpdate: (Int, Int) -> Unit
) {
    var editTotal by remember { mutableStateOf(stats?.totalAmbulances?.toString() ?: "5") }
    var editAvail by remember { mutableStateOf(stats?.availableAmbulances?.toString() ?: "5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Fleet Status", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editTotal,
                    onValueChange = { if (it.all { char -> char.isDigit() }) editTotal = it },
                    label = { Text("Total Units") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = editAvail,
                    onValueChange = { if (it.all { char -> char.isDigit() }) editAvail = it },
                    label = { Text("Available Now") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onUpdate(editTotal.toIntOrNull() ?: 5, editAvail.toIntOrNull() ?: 5) }) {
                Text("Update")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ActionLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Processing...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Logout?") },
        text = { Text("Are you sure you want to logout from the Emergency Operations Center?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
