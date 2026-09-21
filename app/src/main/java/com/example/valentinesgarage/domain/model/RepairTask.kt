package com.example.valentinesgarage.domain.model

/** A single checklist item in a vehicle's active job card. */
data class RepairTask(
    val id: String,
    val vehicleId: String,
    val title: String,
    val status: RepairTaskStatus = RepairTaskStatus.NOT_STARTED,
    val completedByUserId: String? = null,
    val completedByName: String? = null,
    val completedAtEpochMillis: Long? = null,
    val notes: List<RepairNote> = emptyList(),
    val updatedAtEpochMillis: Long
)

data class RepairNote(
    val id: String,
    val text: String,
    val authorUserId: String,
    val authorName: String,
    val createdAtEpochMillis: Long
)

enum class RepairTaskStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}
