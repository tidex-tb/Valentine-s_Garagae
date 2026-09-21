package com.example.valentinesgarage.domain.model

/** A truck registered with the garage. */
data class Vehicle(
    val id: String,
    val licensePlate: String,
    val model: String,
    val status: VehicleStatus = VehicleStatus.CHECKED_IN,
    val currentCheckInId: String? = null,
    val releasedAtEpochMillis: Long? = null,
    val releasedByName: String? = null,
    /**
     * Set when the truck went back to its owner with faults still open - for
     * example a repair the owner declined, or one awaiting a part that had not
     * arrived. Recording this is what separates a deliberate handover from an
     * unfinished job that was simply forgotten.
     */
    val releasedWithOutstandingWork: Boolean = false
)

enum class VehicleStatus(val label: String, val isActive: Boolean) {
    CHECKED_IN("Checked in", true),
    IN_PROGRESS("In progress", true),
    READY_FOR_RELEASE("Ready for release", true),
    RELEASED("Released", false),

    /** Retained so vehicles recorded before release tracking existed still read correctly. */
    COMPLETED("Completed", false)
}
