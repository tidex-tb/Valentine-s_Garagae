package com.example.valentinesgarage.domain.model

/** Append-only audit entry used by the manager activity report. */
data class ActivityLog(
    val id: String,
    val userId: String,
    val userName: String,
    val vehicleId: String,
    val taskId: String? = null,
    val type: ActivityType,
    val description: String,
    val occurredAtEpochMillis: Long
)

enum class ActivityType {
    VEHICLE_CHECKED_IN,
    TASK_STARTED,
    TASK_COMPLETED,
    NOTE_ADDED,
    FAULT_REPORTED,
    FAULT_UPDATED,
    VEHICLE_RELEASED,
    VEHICLE_COMPLETED
}
