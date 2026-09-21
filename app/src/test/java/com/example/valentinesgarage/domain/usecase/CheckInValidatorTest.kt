package com.example.valentinesgarage.domain.usecase

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CheckInValidatorTest {
    private val validator = CheckInValidator()

    @Test fun `accepts a complete intake form`() {
        val result = validator.validate(CheckInForm("N12345W", "Volvo FH", "125000", "Minor scratches on bumper"))
        assertTrue(result.isValid)
    }

    @Test fun `requires evidence fields and a non-negative odometer`() {
        val result = validator.validate(CheckInForm(odometerText = "-12"))
        assertFalse(result.isValid)
        assertTrue(result.licensePlateError != null)
        assertTrue(result.truckModelError != null)
        assertTrue(result.odometerError != null)
        assertTrue(result.conditionError != null)
    }
}
