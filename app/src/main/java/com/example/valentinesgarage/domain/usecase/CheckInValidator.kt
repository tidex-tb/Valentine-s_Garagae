package com.example.valentinesgarage.domain.usecase

data class CheckInForm(
    val licensePlate: String = "",
    val truckModel: String = "",
    val odometerText: String = "",
    val conditionNotes: String = ""
)

data class ValidationResult(
    val licensePlateError: String? = null,
    val truckModelError: String? = null,
    val odometerError: String? = null,
    val conditionError: String? = null
) {
    val isValid: Boolean get() = listOf(licensePlateError, truckModelError, odometerError, conditionError).all { it == null }
}

class CheckInValidator @javax.inject.Inject constructor() {
    fun validate(form: CheckInForm): ValidationResult = ValidationResult(
        licensePlateError = if (form.licensePlate.trim().length < 3) "Enter a valid license plate" else null,
        truckModelError = if (form.truckModel.isBlank()) "Enter the truck model" else null,
        odometerError = when {
            form.odometerText.toLongOrNull() == null -> "Enter kilometers as a whole number"
            form.odometerText.toLong() < 0 -> "Kilometers cannot be negative"
            else -> null
        },
        conditionError = if (form.conditionNotes.isBlank()) "Describe the vehicle condition" else null
    )
}
