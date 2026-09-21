package com.example.valentinesgarage.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.valentinesgarage.domain.model.DamageFlag
import com.example.valentinesgarage.domain.model.GarageUser

@Composable
fun CheckInScreen(user: GarageUser, onBack: () -> Unit, viewModel: CheckInViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); var photoUrl by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Truck check-in", style = MaterialTheme.typography.headlineMedium); TextButton(onClick = onBack) { Text("Back") } }
        Text("Record condition and mileage before any work starts.")
        OutlinedTextField(state.form.licensePlate, { viewModel.updateForm { form -> form.copy(licensePlate = it) } }, Modifier.fillMaxWidth(), label = { Text("License plate / ID") }, isError = state.validation.licensePlateError != null, supportingText = { state.validation.licensePlateError?.let { Text(it) } }, singleLine = true)
        OutlinedTextField(state.form.truckModel, { viewModel.updateForm { form -> form.copy(truckModel = it) } }, Modifier.fillMaxWidth(), label = { Text("Truck model") }, isError = state.validation.truckModelError != null, supportingText = { state.validation.truckModelError?.let { Text(it) } }, singleLine = true)
        OutlinedTextField(state.form.odometerText, { viewModel.updateForm { form -> form.copy(odometerText = it) } }, Modifier.fillMaxWidth(), label = { Text("Odometer (km)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state.validation.odometerError != null, supportingText = { state.validation.odometerError?.let { Text(it) } }, singleLine = true)
        OutlinedTextField(state.form.conditionNotes, { viewModel.updateForm { form -> form.copy(conditionNotes = it) } }, Modifier.fillMaxWidth(), label = { Text("Visual condition notes") }, minLines = 3, isError = state.validation.conditionError != null, supportingText = { state.validation.conditionError?.let { Text(it) } })
        Text("Damage checklist", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { DamageFlag.entries.take(3).forEach { flag -> FilterChip(flag in state.damageFlags, { viewModel.toggleDamage(flag) }, label = { Text(flag.name.replace('_', ' ')) }) } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { DamageFlag.entries.drop(3).forEach { flag -> FilterChip(flag in state.damageFlags, { viewModel.toggleDamage(flag) }, label = { Text(flag.name.replace('_', ' ')) }) } }
        OutlinedTextField(photoUrl, { photoUrl = it }, Modifier.fillMaxWidth(), label = { Text("Condition photo URL (optional)") }, singleLine = true)
        TextButton(onClick = { viewModel.addPhotoUrl(photoUrl); photoUrl = "" }, enabled = photoUrl.isNotBlank()) { Text("Attach photo link") }
        if (state.photoUrls.isNotEmpty()) Text("${state.photoUrls.size} photo link(s) attached")
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }; state.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(onClick = { viewModel.submit(user) }, Modifier.fillMaxWidth(), enabled = !state.isSaving) { Text(if (state.isSaving) "Saving…" else "Check in truck") }
    }
}
