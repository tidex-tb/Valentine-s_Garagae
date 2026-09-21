package com.example.valentinesgarage.data.firestore

import com.example.valentinesgarage.domain.model.ActivityLog
import com.example.valentinesgarage.domain.model.ActivityType
import com.example.valentinesgarage.domain.model.CheckInRecord
import com.example.valentinesgarage.domain.model.DamageFlag
import com.example.valentinesgarage.domain.model.Fault
import com.example.valentinesgarage.domain.model.FaultCategory
import com.example.valentinesgarage.domain.model.FaultResolution
import com.example.valentinesgarage.domain.model.FaultSeverity
import com.example.valentinesgarage.domain.model.FaultSource
import com.example.valentinesgarage.domain.model.RepairNote
import com.example.valentinesgarage.domain.model.RepairTask
import com.example.valentinesgarage.domain.model.RepairTaskStatus
import com.example.valentinesgarage.domain.model.Vehicle
import com.example.valentinesgarage.domain.model.VehicleConditionAssessment
import com.example.valentinesgarage.domain.model.VehicleStatus
import com.example.valentinesgarage.domain.repository.GarageRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreGarageRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : GarageRepository {
    override fun observeActiveVehicles(): Flow<List<Vehicle>> = snapshots(
        firestore.collection(VEHICLES).whereIn("status", VehicleStatus.entries.filter { it.isActive }.map { it.name })
    ).map { documents -> documents.mapNotNull(DocumentSnapshot::toVehicle).sortedBy { it.licensePlate } }

    override fun observeTasks(vehicleId: String): Flow<List<RepairTask>> = snapshots(
        firestore.collection(TASKS).whereEqualTo("vehicleId", vehicleId)
    ).map { documents ->
        // Sort on-device so Firestore does not require a composite index for
        // the first launch of a newly provisioned garage project.
        documents.mapNotNull(DocumentSnapshot::toRepairTask)
            .sortedByDescending { it.updatedAtEpochMillis }
    }

    override fun observeFaults(vehicleId: String): Flow<List<Fault>> = snapshots(
        firestore.collection(FAULTS).whereEqualTo("vehicleId", vehicleId)
    ).map { documents ->
        // Sorted on-device for the same reason as tasks: an equality filter
        // combined with an orderBy would demand a composite index that a newly
        // provisioned garage project does not have.
        documents.mapNotNull(DocumentSnapshot::toFault)
            .sortedWith(compareByDescending<Fault> { it.severity.ordinal }.thenByDescending { it.reportedAtEpochMillis })
    }

    override fun observeActivity(): Flow<List<ActivityLog>> = snapshots(
        firestore.collection(ACTIVITY).orderBy("occurredAtEpochMillis", Query.Direction.DESCENDING)
    ).map { documents -> documents.mapNotNull(DocumentSnapshot::toActivityLog) }

    override fun observeCheckIns(): Flow<List<CheckInRecord>> = snapshots(
        firestore.collection(CHECK_INS).orderBy("intakeDateEpochMillis", Query.Direction.DESCENDING)
    ).map { documents -> documents.mapNotNull(DocumentSnapshot::toCheckInRecord) }

    override suspend fun checkIn(vehicle: Vehicle, record: CheckInRecord): Result<Unit> = runCatching {
        val batch = firestore.batch()
        batch.set(firestore.collection(VEHICLES).document(vehicle.id), vehicle.toMap())
        batch.set(firestore.collection(CHECK_INS).document(record.id), record.toMap())
        standardTasks(vehicle.id, record.createdAtEpochMillis).forEach { task ->
            batch.set(firestore.collection(TASKS).document(task.id), task.toMap())
        }
        batch.set(firestore.collection(ACTIVITY).document(), ActivityLog(
            id = "", userId = record.checkedInByUserId, userName = record.checkedInByName,
            vehicleId = vehicle.id, type = ActivityType.VEHICLE_CHECKED_IN,
            description = "Checked in ${vehicle.licensePlate}", occurredAtEpochMillis = record.createdAtEpochMillis
        ).toMap())
        batch.commit().await()
    }

    override suspend fun setTaskCompleted(task: RepairTask, mechanicId: String, mechanicName: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val completed = task.copy(
            status = RepairTaskStatus.COMPLETED, completedByUserId = mechanicId,
            completedByName = mechanicName, completedAtEpochMillis = now, updatedAtEpochMillis = now
        )
        val batch = firestore.batch()
        batch.set(firestore.collection(TASKS).document(task.id), completed.toMap(), SetOptions.merge())
        batch.set(firestore.collection(ACTIVITY).document(), ActivityLog(
            id = "", userId = mechanicId, userName = mechanicName, vehicleId = task.vehicleId,
            taskId = task.id, type = ActivityType.TASK_COMPLETED, description = "Completed ${task.title}", occurredAtEpochMillis = now
        ).toMap())
        batch.commit().await()
    }

    override suspend fun addTaskNote(task: RepairTask, text: String, mechanicId: String, mechanicName: String): Result<Unit> = runCatching {
        require(text.isNotBlank()) { "A progress note cannot be empty" }
        val now = System.currentTimeMillis()
        val note = RepairNote(firestore.collection(TASKS).document().id, text.trim(), mechanicId, mechanicName, now)
        val updated = task.copy(notes = task.notes + note, status = if (task.status == RepairTaskStatus.NOT_STARTED) RepairTaskStatus.IN_PROGRESS else task.status, updatedAtEpochMillis = now)
        val batch = firestore.batch()
        batch.set(firestore.collection(TASKS).document(task.id), updated.toMap(), SetOptions.merge())
        batch.set(firestore.collection(ACTIVITY).document(), ActivityLog(
            id = "", userId = mechanicId, userName = mechanicName, vehicleId = task.vehicleId,
            taskId = task.id, type = ActivityType.NOTE_ADDED, description = "Added a note to ${task.title}", occurredAtEpochMillis = now
        ).toMap())
        batch.commit().await()
    }

    override suspend fun reportFault(fault: Fault, reporterId: String, reporterName: String): Result<Unit> = runCatching {
        require(fault.description.isNotBlank()) { "Describe the problem before saving it" }
        val batch = firestore.batch()
        batch.set(firestore.collection(FAULTS).document(fault.id), fault.toMap())
        batch.set(firestore.collection(VEHICLES).document(fault.vehicleId), mapOf("status" to VehicleStatus.IN_PROGRESS.name), SetOptions.merge())
        batch.set(firestore.collection(ACTIVITY).document(), ActivityLog(
            id = "", userId = reporterId, userName = reporterName, vehicleId = fault.vehicleId,
            type = ActivityType.FAULT_REPORTED,
            description = "Reported ${fault.category.label.lowercase()} fault: ${fault.description}",
            occurredAtEpochMillis = fault.reportedAtEpochMillis
        ).toMap())
        batch.commit().await()
    }

    override suspend fun updateFaultResolution(
        fault: Fault,
        resolution: FaultResolution,
        notes: String,
        mechanicId: String,
        mechanicName: String
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val updated = fault.copy(resolution = resolution, resolutionNotes = notes.trim(), updatedAtEpochMillis = now)
        val batch = firestore.batch()
        batch.set(firestore.collection(FAULTS).document(fault.id), updated.toMap(), SetOptions.merge())
        batch.set(firestore.collection(ACTIVITY).document(), ActivityLog(
            id = "", userId = mechanicId, userName = mechanicName, vehicleId = fault.vehicleId,
            type = ActivityType.FAULT_UPDATED,
            description = "${fault.category.label} fault marked ${resolution.label.lowercase()}",
            occurredAtEpochMillis = now
        ).toMap())
        batch.commit().await()
    }

    override suspend fun releaseVehicle(
        vehicle: Vehicle,
        outstandingFaults: List<Fault>,
        releasedById: String,
        releasedByName: String
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val batch = firestore.batch()
        batch.set(firestore.collection(VEHICLES).document(vehicle.id), mapOf(
            "status" to VehicleStatus.RELEASED.name,
            "releasedAtEpochMillis" to now,
            "releasedByName" to releasedByName,
            "releasedWithOutstandingWork" to outstandingFaults.isNotEmpty()
        ), SetOptions.merge())
        val summary = if (outstandingFaults.isEmpty()) {
            "Released ${vehicle.licensePlate} with all work complete"
        } else {
            "Released ${vehicle.licensePlate} with ${outstandingFaults.size} outstanding item(s): " +
                outstandingFaults.joinToString { "${it.category.label} (${it.resolution.label.lowercase()})" }
        }
        batch.set(firestore.collection(ACTIVITY).document(), ActivityLog(
            id = "", userId = releasedById, userName = releasedByName, vehicleId = vehicle.id,
            type = ActivityType.VEHICLE_RELEASED, description = summary, occurredAtEpochMillis = now
        ).toMap())
        batch.commit().await()
    }

    private fun snapshots(query: Query): Flow<List<DocumentSnapshot>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) close(error) else trySend(snapshot?.documents.orEmpty())
        }
        awaitClose { registration.remove() }
    }

    /** Provides a consistent starting checklist so work cannot be missed after intake. */
    private fun standardTasks(vehicleId: String, createdAt: Long): List<RepairTask> = listOf(
        "Oil change and fluid inspection",
        "Brake pad and tyre inspection",
        "Engine diagnostics"
    ).map { title ->
        RepairTask(
            id = firestore.collection(TASKS).document().id,
            vehicleId = vehicleId,
            title = title,
            updatedAtEpochMillis = createdAt
        )
    }

    private companion object { const val VEHICLES = "vehicles"; const val CHECK_INS = "checkIns"; const val TASKS = "repairTasks"; const val ACTIVITY = "activityLogs"; const val FAULTS = "faults" }
}

private fun Vehicle.toMap() = mapOf("licensePlate" to licensePlate, "model" to model, "status" to status.name, "currentCheckInId" to currentCheckInId, "releasedAtEpochMillis" to releasedAtEpochMillis, "releasedByName" to releasedByName, "releasedWithOutstandingWork" to releasedWithOutstandingWork)
private fun CheckInRecord.toMap() = mapOf("vehicleId" to vehicleId, "intakeDateEpochMillis" to intakeDateEpochMillis, "initialOdometerKm" to initialOdometerKm, "visualNotes" to condition.visualNotes, "damageFlags" to condition.damageFlags.map { it.name }, "photoUrls" to photoUrls, "checkedInByUserId" to checkedInByUserId, "checkedInByName" to checkedInByName, "createdAtEpochMillis" to createdAtEpochMillis)
private fun RepairTask.toMap() = mapOf("vehicleId" to vehicleId, "title" to title, "status" to status.name, "completedByUserId" to completedByUserId, "completedByName" to completedByName, "completedAtEpochMillis" to completedAtEpochMillis, "notes" to notes.map { mapOf("id" to it.id, "text" to it.text, "authorUserId" to it.authorUserId, "authorName" to it.authorName, "createdAtEpochMillis" to it.createdAtEpochMillis) }, "updatedAtEpochMillis" to updatedAtEpochMillis)
private fun ActivityLog.toMap() = mapOf("userId" to userId, "userName" to userName, "vehicleId" to vehicleId, "taskId" to taskId, "type" to type.name, "description" to description, "occurredAtEpochMillis" to occurredAtEpochMillis)
private fun DocumentSnapshot.toVehicle() = getString("licensePlate")?.let { Vehicle(id, it, getString("model").orEmpty(), enum("status", VehicleStatus.CHECKED_IN), getString("currentCheckInId"), getLong("releasedAtEpochMillis"), getString("releasedByName"), getBoolean("releasedWithOutstandingWork") ?: false) }

private fun Fault.toMap() = mapOf("vehicleId" to vehicleId, "category" to category.name, "severity" to severity.name, "source" to source.name, "description" to description, "resolution" to resolution.name, "resolutionNotes" to resolutionNotes, "reportedByUserId" to reportedByUserId, "reportedByName" to reportedByName, "reportedAtEpochMillis" to reportedAtEpochMillis, "updatedAtEpochMillis" to updatedAtEpochMillis)

private fun DocumentSnapshot.toFault() = getString("description")?.let { description ->
    Fault(
        id = id,
        vehicleId = getString("vehicleId").orEmpty(),
        category = enum("category", FaultCategory.OTHER),
        severity = enum("severity", FaultSeverity.MINOR),
        source = enum("source", FaultSource.DISCOVERED_DURING_WORK),
        description = description,
        resolution = enum("resolution", FaultResolution.PENDING),
        resolutionNotes = getString("resolutionNotes").orEmpty(),
        reportedByUserId = getString("reportedByUserId").orEmpty(),
        reportedByName = getString("reportedByName").orEmpty(),
        reportedAtEpochMillis = getLong("reportedAtEpochMillis") ?: 0L,
        updatedAtEpochMillis = getLong("updatedAtEpochMillis") ?: 0L
    )
}
private fun DocumentSnapshot.toRepairTask() = getString("title")?.let { title -> RepairTask(id, getString("vehicleId").orEmpty(), title, enum("status", RepairTaskStatus.NOT_STARTED), getString("completedByUserId"), getString("completedByName"), getLong("completedAtEpochMillis"), (get("notes") as? List<Map<String, Any?>>).orEmpty().mapNotNull { note -> (note["text"] as? String)?.let { RepairNote(note["id"] as? String ?: "", it, note["authorUserId"] as? String ?: "", note["authorName"] as? String ?: "", note["createdAtEpochMillis"] as? Long ?: 0L) } }, getLong("updatedAtEpochMillis") ?: 0L) }
private fun DocumentSnapshot.toActivityLog() = getString("description")?.let { ActivityLog(id, getString("userId").orEmpty(), getString("userName").orEmpty(), getString("vehicleId").orEmpty(), getString("taskId"), enum("type", ActivityType.NOTE_ADDED), it, getLong("occurredAtEpochMillis") ?: 0L) }
private fun DocumentSnapshot.toCheckInRecord() = getString("vehicleId")?.let { vehicleId -> CheckInRecord(id, vehicleId, getLong("intakeDateEpochMillis") ?: 0L, getLong("initialOdometerKm") ?: 0L, VehicleConditionAssessment(getString("visualNotes").orEmpty(), (get("damageFlags") as? List<String>).orEmpty().mapNotNull { runCatching { DamageFlag.valueOf(it) }.getOrNull() }.toSet()), (get("photoUrls") as? List<String>).orEmpty(), getString("checkedInByUserId").orEmpty(), getString("checkedInByName").orEmpty(), getLong("createdAtEpochMillis") ?: 0L) }
private inline fun <reified T : Enum<T>> DocumentSnapshot.enum(field: String, fallback: T): T = runCatching { enumValueOf<T>(getString(field).orEmpty()) }.getOrDefault(fallback)
