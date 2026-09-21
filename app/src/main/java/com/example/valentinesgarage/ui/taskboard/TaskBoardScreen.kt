package com.example.valentinesgarage.ui.taskboard

import androidx.compose.foundation.clickable
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
import com.example.valentinesgarage.ui.components.*

@Composable
fun TaskListScreen(user: GarageUser, onOpenVehicle: (String) -> Unit, onCheckIn: () -> Unit, onReports: () -> Unit, onSignOut: () -> Unit, viewModel: ActiveVehiclesViewModel = hiltViewModel()) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { GarageBrand(); IconButton(onClick = onSignOut) { Icon(Icons.Outlined.Logout, "Sign out") } } }
        item { Column { Text("Workshop board", style = MaterialTheme.typography.headlineLarge); Text("Real-time repair progress across every active truck", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        if (user.role == UserRole.MANAGER) item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { ElevatedButton(onClick = onCheckIn) { Icon(Icons.Outlined.AddCircle, null); Text("  Check in truck") }; AssistChip(onClick = onReports, label = { Text("View reports") }) } }
        errorMessage?.let { message -> item { StreamErrorBanner(message) } }
        item { Text("ACTIVE TRUCKS  ·  ${vehicles.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(vehicles, key = { it.id }) { vehicle ->
            Card(Modifier.fillMaxWidth().clickable { onOpenVehicle(vehicle.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(vehicle.licensePlate, style = MaterialTheme.typography.titleLarge); Text(vehicle.model, color = MaterialTheme.colorScheme.onSurfaceVariant) }; StatusPill(vehicle.status) }
            }
        }
        if (vehicles.isEmpty()) item { EmptyWorkshopState(user.role == UserRole.MANAGER, onCheckIn) }
    }
}

@Composable
fun TaskBoardScreen(user: GarageUser, onBack: () -> Unit, viewModel: TaskBoardViewModel = hiltViewModel()) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle(); val complete = tasks.count { it.status == RepairTaskStatus.COMPLETED }
    val faults by viewModel.faults.collectAsStateWithLifecycle()
    val outstanding by viewModel.outstandingFaults.collectAsStateWithLifecycle()
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    var reportingFault by remember { mutableStateOf(false) }
    var editingFault by remember { mutableStateOf<Fault?>(null) }
    var releasing by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
                Column(Modifier.padding(start = 8.dp).weight(1f)) {
                    Text(vehicle?.licensePlate ?: "Repair job card", style = MaterialTheme.typography.headlineMedium)
                    Text("$complete of ${tasks.size} service checks  ·  ${outstanding.size} open problem(s)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                vehicle?.let { StatusPill(it.status) }
            }
        }
        errorMessage?.let { message -> item { StreamErrorBanner(message) } }
        actionMessage?.let { message ->
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = viewModel::dismissActionMessage) { Text("Dismiss") }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                ElevatedButton(onClick = { reportingFault = true }) { Icon(Icons.Outlined.ReportProblem, null); Text("  Report a problem") }
                vehicle?.let { current ->
                    if (current.status.isActive) {
                        Button(onClick = { releasing = true }) { Icon(Icons.Outlined.AssignmentTurnedIn, null); Text("  Release") }
                    }
                }
            }
        }

        item { SectionLabel("REPORTED PROBLEMS  ·  ${faults.size}") }
        items(faults, key = { "fault-${it.id}" }) { fault -> FaultCard(fault) { editingFault = fault } }
        if (faults.isEmpty()) item {
            Text("No problems recorded for this truck. Add one as soon as an inspection turns something up.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item { SectionLabel("SERVICE CHECKLIST") }
        items(tasks, key = { it.id }) { task -> TaskCard(task, user, viewModel::complete, viewModel::addNote) }
        if (tasks.isEmpty()) item { Text("No repair tasks have been assigned yet.", modifier = Modifier.padding(24.dp)) }

        vehicle?.let { current ->
            if (!current.status.isActive) item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Released${current.releasedByName?.let { name -> " by $name" }.orEmpty()}", style = MaterialTheme.typography.titleMedium)
                        if (current.releasedWithOutstandingWork) Text("Handed back with work still outstanding.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (reportingFault) ReportFaultDialog(
        onDismiss = { reportingFault = false },
        onSubmit = { category, severity, source, description -> viewModel.reportFault(category, severity, source, description, user) }
    )
    editingFault?.let { fault ->
        UpdateFaultDialog(
            fault = fault,
            onDismiss = { editingFault = null },
            onSubmit = { resolution, notes -> viewModel.updateFault(fault, resolution, notes, user) }
        )
    }
    if (releasing) vehicle?.let { current ->
        ReleaseVehicleDialog(
            licensePlate = current.licensePlate,
            outstandingFaults = outstanding,
            onDismiss = { releasing = false },
            onConfirm = { viewModel.release(user) }
        )
    }
}

@Composable
private fun SectionLabel(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))

@Composable
private fun FaultCard(fault: Fault, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onEdit), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(fault.category.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                SeverityPill(fault.severity)
            }
            Text(fault.description, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ResolutionPill(fault.resolution)
                Text(fault.source.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (fault.resolutionNotes.isNotBlank()) Text(fault.resolutionNotes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TaskCard(task: RepairTask, user: GarageUser, onComplete: (RepairTask, GarageUser) -> Unit, onAddNote: (RepairTask, String, GarageUser) -> Unit) {
    var note by remember(task.id) { mutableStateOf("") }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(task.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); TaskStatusPill(task.status) }
        task.completedByName?.let { Text("Completed by $it", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall) }
        if (task.notes.isNotEmpty()) { HorizontalDivider(); task.notes.takeLast(2).forEach { Text("${it.authorName}  ·  ${it.text}", style = MaterialTheme.typography.bodyMedium) } }
        if (task.status != RepairTaskStatus.COMPLETED) Button(onClick = { onComplete(task, user) }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.CheckCircle, null); Text("  Mark task complete") }
        OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Add a progress note") }, minLines = 2)
        TextButton(onClick = { onAddNote(task, note, user); note = "" }, enabled = note.isNotBlank(), modifier = Modifier.align(Alignment.End)) { Text("Save note") }
    } }
}

@Composable
private fun EmptyWorkshopState(canCheckIn: Boolean, onCheckIn: () -> Unit) = Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary); Text("The workshop is clear", style = MaterialTheme.typography.titleLarge); Text("Active trucks will appear here as soon as they are checked in.", color = MaterialTheme.colorScheme.onSurfaceVariant); if (canCheckIn) Button(onClick = onCheckIn) { Text("Check in first truck") } } }
