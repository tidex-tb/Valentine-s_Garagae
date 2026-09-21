package com.example.valentinesgarage.ui.taskboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.valentinesgarage.domain.model.Fault
import com.example.valentinesgarage.domain.model.FaultCategory
import com.example.valentinesgarage.domain.model.FaultResolution
import com.example.valentinesgarage.domain.model.FaultSeverity
import com.example.valentinesgarage.domain.model.FaultSource

/**
 * Captures a problem found with a truck. Reachable at any point in the job, not
 * only at intake, so a fault discovered halfway through a service has somewhere
 * to go.
 */
@Composable
fun ReportFaultDialog(
    onDismiss: () -> Unit,
    onSubmit: (FaultCategory, FaultSeverity, FaultSource, String) -> Unit
) {
    var category by remember { mutableStateOf(FaultCategory.ENGINE) }
    var severity by remember { mutableStateOf(FaultSeverity.MAJOR) }
    var source by remember { mutableStateOf(FaultSource.DISCOVERED_DURING_WORK) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report a problem") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChipPicker("System affected", FaultCategory.entries, category, { it.label }) { category = it }
                ChipPicker("Severity", FaultSeverity.entries, severity, { it.label }) { severity = it }
                ChipPicker("How it was found", FaultSource.entries, source, { it.label }) { source = it }
                OutlinedTextField(
                    description, { description = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("What is wrong?") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(category, severity, source, description); onDismiss() },
                enabled = description.isNotBlank()
            ) { Text("Save problem") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Moves a fault to a new outcome, including the outcomes where no repair happened. */
@Composable
fun UpdateFaultDialog(
    fault: Fault,
    onDismiss: () -> Unit,
    onSubmit: (FaultResolution, String) -> Unit
) {
    var resolution by remember(fault.id) { mutableStateOf(fault.resolution) }
    var notes by remember(fault.id) { mutableStateOf(fault.resolutionNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(fault.category.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(fault.description, style = MaterialTheme.typography.bodyMedium)
                ChipPicker("Still open", FaultResolution.openStates, resolution, { it.label }) { resolution = it }
                ChipPicker("Closing out", FaultResolution.closingStates, resolution, { it.label }) { resolution = it }
                OutlinedTextField(
                    notes, { notes = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Findings / why") },
                    minLines = 2
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(resolution, notes); onDismiss() }) { Text("Update") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Releasing with open faults is allowed but never silent: the outstanding items
 * are listed and acknowledged, so the handover is a recorded decision.
 */
@Composable
fun ReleaseVehicleDialog(
    licensePlate: String,
    outstandingFaults: List<Fault>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (outstandingFaults.isEmpty()) "Release $licensePlate" else "Release with open work?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (outstandingFaults.isEmpty()) {
                    Text("All recorded work on $licensePlate is complete. This hands the truck back to its owner.")
                } else {
                    Text("$licensePlate still has ${outstandingFaults.size} unresolved item(s):", style = MaterialTheme.typography.titleMedium)
                    outstandingFaults.forEach { fault ->
                        Text("•  ${fault.category.label} — ${fault.resolution.label.lowercase()}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Releasing now records that the owner took the truck back with this work outstanding.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(if (outstandingFaults.isEmpty()) "Release truck" else "Release anyway")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun <T> ChipPicker(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                FilterChip(option == selected, { onSelect(option) }, label = { Text(optionLabel(option)) }, modifier = Modifier.padding(end = 2.dp))
            }
        }
    }
}
