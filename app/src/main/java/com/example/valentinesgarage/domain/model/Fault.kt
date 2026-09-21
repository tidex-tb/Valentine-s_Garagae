package com.example.valentinesgarage.domain.model

/**
 * A specific problem with a vehicle, tracked independently of the checklist
 * work done about it.
 *
 * A fault is not the same thing as a [RepairTask]. Tasks are the routine
 * service checklist every truck receives; a fault is something actually wrong
 * with this truck - a slipping transmission, a cracked windscreen, an engine
 * that will not start. Faults carry their own outcome so the workshop can
 * record that something was diagnosed but could not be repaired, or that the
 * owner declined the work, rather than silently leaving it incomplete.
 */
data class Fault(
    val id: String,
    val vehicleId: String,
    val category: FaultCategory,
    val severity: FaultSeverity,
    val source: FaultSource,
    val description: String,
    val resolution: FaultResolution = FaultResolution.PENDING,
    val resolutionNotes: String = "",
    val reportedByUserId: String,
    val reportedByName: String,
    val reportedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
) {
    /** True while this fault still represents work the garage owes the vehicle. */
    val isOutstanding: Boolean get() = resolution.isOutstanding
}

enum class FaultCategory(val label: String) {
    ENGINE("Engine"),
    TRANSMISSION("Transmission & drivetrain"),
    BRAKES("Brakes"),
    SUSPENSION("Suspension & steering"),
    ELECTRICAL("Electrical"),
    COOLING("Cooling & overheating"),
    FUEL("Fuel system"),
    BODYWORK("Bodywork"),
    GLASS("Glass & windscreen"),
    TYRES("Tyres & wheels"),
    SERVICE("Routine service"),
    OTHER("Other")
}

enum class FaultSeverity(val label: String) {
    ADVISORY("Advisory"),
    MINOR("Minor"),
    MAJOR("Major"),
    SAFETY_CRITICAL("Safety critical")
}

/**
 * Where the fault came from. [DISCOVERED_DURING_WORK] is what allows a truck to
 * be booked in on a short description and have real problems added later, once
 * a mechanic has actually inspected it.
 */
enum class FaultSource(val label: String) {
    CUSTOMER_REPORTED("Reported by owner"),
    INTAKE_INSPECTION("Found at intake"),
    DISCOVERED_DURING_WORK("Discovered during work")
}

/**
 * How a fault ended. The closed outcomes are deliberately distinct: a garage
 * needs to tell apart work it could not do from work the owner would not pay
 * for, and both from a fault that turned out not to exist.
 */
enum class FaultResolution(val label: String, val isOutstanding: Boolean) {
    PENDING("Not started", true),
    DIAGNOSING("Diagnosing", true),
    IN_PROGRESS("Repair in progress", true),
    AWAITING_PARTS("Awaiting parts", true),
    AWAITING_APPROVAL("Awaiting owner approval", true),
    REPAIRED("Repaired", false),
    UNREPAIRABLE("Could not be repaired", false),
    DECLINED("Declined by owner", false),
    DEFERRED("Deferred to next visit", false),
    NOT_FOUND("Fault not found", false);

    companion object {
        /** Offered while a fault is still open. */
        val openStates: List<FaultResolution> get() = entries.filter { it.isOutstanding }

        /** Offered when closing a fault out. */
        val closingStates: List<FaultResolution> get() = entries.filter { !it.isOutstanding }
    }
}
