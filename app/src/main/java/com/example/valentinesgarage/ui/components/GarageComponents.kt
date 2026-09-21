package com.example.valentinesgarage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.valentinesgarage.domain.model.FaultResolution
import com.example.valentinesgarage.domain.model.FaultSeverity
import com.example.valentinesgarage.domain.model.RepairTaskStatus
import com.example.valentinesgarage.domain.model.VehicleStatus
import com.example.valentinesgarage.ui.theme.GarageError
import com.example.valentinesgarage.ui.theme.GarageSuccess

@Composable
fun GarageBrand(mark: String = "VG") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(mark, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(horizontal = 11.dp, vertical = 8.dp))
        Spacer(Modifier.width(10.dp))
        Column { Text("VALENTINE'S", style = MaterialTheme.typography.labelLarge); Text("GARAGE", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
fun StatusPill(status: VehicleStatus) = StatusPill(status.label, when (status) {
    VehicleStatus.COMPLETED, VehicleStatus.RELEASED -> GarageSuccess
    VehicleStatus.READY_FOR_RELEASE -> GarageSuccess
    VehicleStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
    VehicleStatus.CHECKED_IN -> MaterialTheme.colorScheme.secondary
})

@Composable
fun SeverityPill(severity: FaultSeverity) = StatusPill(severity.label, when (severity) {
    FaultSeverity.SAFETY_CRITICAL, FaultSeverity.MAJOR -> GarageError
    FaultSeverity.MINOR -> MaterialTheme.colorScheme.primary
    FaultSeverity.ADVISORY -> MaterialTheme.colorScheme.secondary
})

@Composable
fun ResolutionPill(resolution: FaultResolution) = StatusPill(resolution.label, when (resolution) {
    FaultResolution.REPAIRED -> GarageSuccess
    FaultResolution.UNREPAIRABLE, FaultResolution.DECLINED -> GarageError
    FaultResolution.DEFERRED, FaultResolution.NOT_FOUND -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.primary
})

@Composable
fun TaskStatusPill(status: RepairTaskStatus) = StatusPill(status.name.replace('_', ' '), when (status) { RepairTaskStatus.COMPLETED -> GarageSuccess; RepairTaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary; RepairTaskStatus.NOT_STARTED -> MaterialTheme.colorScheme.secondary })

@Composable
private fun StatusPill(label: String, color: Color) {
    Text(label, color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clip(CircleShape).background(color.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 6.dp))
}

@Composable
fun MetricCard(value: String, label: String, icon: ImageVector = Icons.Outlined.Build) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Explains a failed live-data stream in place, so a Firestore problem degrades
 * the screen instead of taking the app down. Renders nothing when [message] is null.
 */
@Composable
fun StreamErrorBanner(message: String?, modifier: Modifier = Modifier) {
    if (message == null) return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GarageError.copy(alpha = .10f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CloudOff, null, tint = GarageError, modifier = Modifier.size(22.dp))
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Live data unavailable", style = MaterialTheme.typography.titleMedium, color = GarageError)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
