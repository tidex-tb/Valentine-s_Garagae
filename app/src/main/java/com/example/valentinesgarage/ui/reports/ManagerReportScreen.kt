package com.example.valentinesgarage.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.valentinesgarage.domain.model.*
import com.example.valentinesgarage.ui.components.GarageBrand
import com.example.valentinesgarage.ui.components.MetricCard
import com.example.valentinesgarage.ui.components.StatusPill
import com.example.valentinesgarage.ui.components.StreamErrorBanner

@Composable
fun ManagerReportScreen(user: GarageUser, onTasks: () -> Unit, onCheckIn: () -> Unit, onSignOut: () -> Unit, viewModel: ManagerReportViewModel = hiltViewModel()) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle(); val activity by viewModel.activity.collectAsStateWithLifecycle(); val checkIns by viewModel.checkIns.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    if (user.role != UserRole.MANAGER) { Column(Modifier.fillMaxSize().padding(24.dp)) { Text("Manager access required"); TextButton(onClick = onTasks) { Text("Go to task board") } }; return }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { GarageBrand(); IconButton(onClick = onSignOut) { Icon(Icons.Outlined.Logout, "Sign out") } } }
        item { Text("Good day, ${user.displayName}", style = MaterialTheme.typography.headlineLarge) }
        item { Text("Live operations overview for your workshop", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = onCheckIn) { Icon(Icons.Outlined.AddCircle, null); Text("  New check-in") }; AssistChip(onClick = onTasks, label = { Text("Workshop board") }, leadingIcon = { Icon(Icons.Outlined.Build, null) }) } }
        errorMessage?.let { message -> item { StreamErrorBanner(message) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Box(Modifier.weight(1f)) { MetricCard(vehicles.size.toString(), "Active trucks") }; Box(Modifier.weight(1f)) { MetricCard(activity.count { it.type == ActivityType.TASK_COMPLETED }.toString(), "Tasks completed") } } }
        item { Text("INTAKE SUMMARY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
        items(checkIns.take(5), key = { "checkin-${it.id}" }) { record ->
            val vehicle = vehicles.firstOrNull { it.id == record.vehicleId }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(vehicle?.let { "${it.licensePlate} · ${it.model}" } ?: "Vehicle ${record.vehicleId.take(8)}", style = MaterialTheme.typography.titleMedium); vehicle?.let { StatusPill(it.status) } }
                Text("${record.initialOdometerKm} km at intake", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
                Text(record.condition.visualNotes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (record.condition.damageFlags.isNotEmpty()) AssistChip(onClick = {}, label = { Text(record.condition.damageFlags.joinToString { it.name.replace('_', ' ') }) })
            } }
        }
        if (checkIns.isEmpty()) item { Text("No check-ins recorded yet.", modifier = Modifier.padding(vertical = 12.dp)) }
        item { Text("RECENT EMPLOYEE ACTIVITY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp)) }
        items(activity.take(8), key = { "activity-${it.id}" }) { log -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.padding(start = 12.dp).weight(1f)) { Text(log.userName, style = MaterialTheme.typography.titleMedium); Text(log.description, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(log.occurredAtEpochMillis)), style = MaterialTheme.typography.bodySmall) } } } }
    }
}
