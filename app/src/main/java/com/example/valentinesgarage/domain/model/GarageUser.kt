package com.example.valentinesgarage.domain.model

/** Profile data stored separately from Firebase Authentication credentials. */
data class GarageUser(
    val id: String,
    val displayName: String,
    val email: String,
    val role: UserRole
)
