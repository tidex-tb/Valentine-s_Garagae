package com.example.valentinesgarage.domain.repository

import com.example.valentinesgarage.domain.model.ActivityLog
import com.example.valentinesgarage.domain.model.CheckInRecord
import com.example.valentinesgarage.domain.model.Fault
import com.example.valentinesgarage.domain.model.FaultResolution
import com.example.valentinesgarage.domain.model.RepairTask
import com.example.valentinesgarage.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface GarageRepository {
    fun observeActiveVehicles(): Flow<List<Vehicle>>
    fun observeTasks(vehicleId: String): Flow<List<RepairTask>>
    fun observeFaults(vehicleId: String): Flow<List<Fault>>
    fun observeActivity(): Flow<List<ActivityLog>>
    fun observeCheckIns(): Flow<List<CheckInRecord>>

    suspend fun checkIn(vehicle: Vehicle, record: CheckInRecord): Result<Unit>
    suspend fun setTaskCompleted(task: RepairTask, mechanicId: String, mechanicName: String): Result<Unit>
    suspend fun addTaskNote(task: RepairTask, text: String, mechanicId: String, mechanicName: String): Result<Unit>

    /** Records a problem found with a vehicle, at intake or at any point afterwards. */
    suspend fun reportFault(fault: Fault, reporterId: String, reporterName: String): Result<Unit>

    /** Moves a fault to a new outcome, capturing why. */
    suspend fun updateFaultResolution(
        fault: Fault,
        resolution: FaultResolution,
        notes: String,
        mechanicId: String,
        mechanicName: String
    ): Result<Unit>

    /**
     * Hands a vehicle back to its owner. [outstandingFaults] is recorded rather
     * than rejected, so a release with unfinished work is an auditable decision
     * instead of an inconsistent job card.
     */
    suspend fun releaseVehicle(
        vehicle: Vehicle,
        outstandingFaults: List<Fault>,
        releasedById: String,
        releasedByName: String
    ): Result<Unit>
}
