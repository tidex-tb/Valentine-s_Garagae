package com.example.valentinesgarage.domain.model

/** Immutable intake evidence for a vehicle visit. All times are UTC epoch milliseconds. */
data class CheckInRecord(
    val id: String,
    val vehicleId: String,
    val intakeDateEpochMillis: Long,
    val initialOdometerKm: Long,
    val condition: VehicleConditionAssessment,
    val photoUrls: List<String> = emptyList(),
    val checkedInByUserId: String,
    val checkedInByName: String,
    val createdAtEpochMillis: Long
)

data class VehicleConditionAssessment(
    val visualNotes: String,
    val damageFlags: Set<DamageFlag> = emptySet()
)

enum class DamageFlag {
    BODY_DAMAGE,
    WINDSCREEN_DAMAGE,
    LIGHT_DAMAGE,
    TYRE_DAMAGE,
    FLUID_LEAK,
    OTHER
}
