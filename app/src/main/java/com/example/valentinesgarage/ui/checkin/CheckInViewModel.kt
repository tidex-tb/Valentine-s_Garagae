package com.example.valentinesgarage.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.valentinesgarage.domain.model.CheckInRecord
import com.example.valentinesgarage.domain.model.DamageFlag
import com.example.valentinesgarage.domain.model.GarageUser
import com.example.valentinesgarage.domain.model.Vehicle
import com.example.valentinesgarage.domain.model.VehicleConditionAssessment
import com.example.valentinesgarage.domain.repository.GarageRepository
import com.example.valentinesgarage.domain.usecase.CheckInForm
import com.example.valentinesgarage.domain.usecase.CheckInValidator
import com.example.valentinesgarage.domain.usecase.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CheckInUiState(val form: CheckInForm = CheckInForm(), val damageFlags: Set<DamageFlag> = emptySet(), val photoUrls: List<String> = emptyList(), val validation: ValidationResult = ValidationResult(), val isSaving: Boolean = false, val successMessage: String? = null, val errorMessage: String? = null)

@HiltViewModel
class CheckInViewModel @Inject constructor(private val repository: GarageRepository, private val validator: CheckInValidator = CheckInValidator()) : ViewModel() {
    private val mutableState = MutableStateFlow(CheckInUiState())
    val state: StateFlow<CheckInUiState> = mutableState.asStateFlow()
    fun updateForm(transform: (CheckInForm) -> CheckInForm) { mutableState.value = mutableState.value.copy(form = transform(mutableState.value.form), validation = ValidationResult(), successMessage = null, errorMessage = null) }
    fun toggleDamage(flag: DamageFlag) { mutableState.value = mutableState.value.let { it.copy(damageFlags = it.damageFlags.let { flags -> if (flag in flags) flags - flag else flags + flag }) } }
    fun addPhotoUrl(url: String) { if (url.isNotBlank()) mutableState.value = mutableState.value.copy(photoUrls = mutableState.value.photoUrls + url.trim()) }
    fun submit(user: GarageUser) {
        val current = mutableState.value; val validation = validator.validate(current.form)
        if (!validation.isValid) { mutableState.value = current.copy(validation = validation); return }
        viewModelScope.launch {
            mutableState.value = current.copy(isSaving = true)
            val now = System.currentTimeMillis(); val vehicleId = UUID.randomUUID().toString(); val recordId = UUID.randomUUID().toString()
            val result = repository.checkIn(Vehicle(vehicleId, current.form.licensePlate.trim().uppercase(), current.form.truckModel.trim(), currentCheckInId = recordId), CheckInRecord(recordId, vehicleId, now, current.form.odometerText.toLong(), VehicleConditionAssessment(current.form.conditionNotes.trim(), current.damageFlags), current.photoUrls, user.id, user.displayName, now))
            mutableState.value = CheckInUiState(successMessage = if (result.isSuccess) "Truck checked in successfully" else null, errorMessage = result.exceptionOrNull()?.message)
        }
    }
}
