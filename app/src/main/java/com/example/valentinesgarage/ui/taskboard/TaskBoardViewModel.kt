package com.example.valentinesgarage.ui.taskboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.valentinesgarage.domain.model.Fault
import com.example.valentinesgarage.domain.model.FaultCategory
import com.example.valentinesgarage.domain.model.FaultResolution
import com.example.valentinesgarage.domain.model.FaultSeverity
import com.example.valentinesgarage.domain.model.FaultSource
import com.example.valentinesgarage.domain.model.GarageUser
import com.example.valentinesgarage.domain.model.RepairTask
import com.example.valentinesgarage.domain.model.Vehicle
import com.example.valentinesgarage.domain.repository.GarageRepository
import com.example.valentinesgarage.ui.common.asGarageStream
import com.example.valentinesgarage.ui.common.combineErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskBoardViewModel @Inject constructor(
    private val repository: GarageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val vehicleId: String = checkNotNull(savedStateHandle["vehicleId"])

    private val taskStream = repository.observeTasks(vehicleId).asGarageStream(viewModelScope)
    private val faultStream = repository.observeFaults(vehicleId).asGarageStream(viewModelScope)

    val tasks: StateFlow<List<RepairTask>> = taskStream.items
    val faults: StateFlow<List<Fault>> = faultStream.items
    val errorMessage: StateFlow<String?> = combineErrors(viewModelScope, listOf(taskStream, faultStream))

    /**
     * Read from the active-vehicle stream rather than a dedicated document
     * listener, so releasing does not add a second query per job card.
     */
    val vehicle: StateFlow<Vehicle?> = repository.observeActiveVehicles()
        .map { vehicles -> vehicles.firstOrNull { it.id == vehicleId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val mutableActionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = mutableActionMessage.asStateFlow()
    fun dismissActionMessage() { mutableActionMessage.value = null }

    /** Faults the garage still owes this vehicle. Drives the release warning. */
    val outstandingFaults: StateFlow<List<Fault>> = faults
        .map { list -> list.filter(Fault::isOutstanding) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun complete(task: RepairTask, user: GarageUser) = viewModelScope.launch {
        repository.setTaskCompleted(task, user.id, user.displayName).reportFailure()
    }

    fun addNote(task: RepairTask, note: String, user: GarageUser) = viewModelScope.launch {
        repository.addTaskNote(task, note, user.id, user.displayName).reportFailure()
    }

    fun reportFault(
        category: FaultCategory,
        severity: FaultSeverity,
        source: FaultSource,
        description: String,
        user: GarageUser
    ) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val fault = Fault(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            category = category,
            severity = severity,
            source = source,
            description = description.trim(),
            reportedByUserId = user.id,
            reportedByName = user.displayName,
            reportedAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        repository.reportFault(fault, user.id, user.displayName).reportFailure()
    }

    fun updateFault(fault: Fault, resolution: FaultResolution, notes: String, user: GarageUser) = viewModelScope.launch {
        repository.updateFaultResolution(fault, resolution, notes, user.id, user.displayName).reportFailure()
    }

    fun release(user: GarageUser) = viewModelScope.launch {
        val current = vehicle.value ?: run {
            mutableActionMessage.value = "This truck is no longer open for release."
            return@launch
        }
        repository.releaseVehicle(current, outstandingFaults.value, user.id, user.displayName)
            .onSuccess { mutableActionMessage.value = "${current.licensePlate} released." }
            .reportFailure()
    }

    private fun Result<Unit>.reportFailure() = onFailure { throwable ->
        mutableActionMessage.value = throwable.message ?: "That change could not be saved."
    }
}
