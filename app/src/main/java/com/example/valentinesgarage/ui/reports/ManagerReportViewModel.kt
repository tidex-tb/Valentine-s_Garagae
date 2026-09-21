package com.example.valentinesgarage.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.valentinesgarage.domain.model.ActivityLog
import com.example.valentinesgarage.domain.model.CheckInRecord
import com.example.valentinesgarage.domain.model.Vehicle
import com.example.valentinesgarage.domain.repository.GarageRepository
import com.example.valentinesgarage.ui.common.asGarageStream
import com.example.valentinesgarage.ui.common.combineErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ManagerReportViewModel @Inject constructor(repository: GarageRepository) : ViewModel() {
    private val vehicleStream = repository.observeActiveVehicles().asGarageStream(viewModelScope)
    private val activityStream = repository.observeActivity().asGarageStream(viewModelScope)
    private val checkInStream = repository.observeCheckIns().asGarageStream(viewModelScope)

    val vehicles: StateFlow<List<Vehicle>> = vehicleStream.items
    val activity: StateFlow<List<ActivityLog>> = activityStream.items
    val checkIns: StateFlow<List<CheckInRecord>> = checkInStream.items
    val errorMessage: StateFlow<String?> = combineErrors(viewModelScope, listOf(vehicleStream, activityStream, checkInStream))
}
