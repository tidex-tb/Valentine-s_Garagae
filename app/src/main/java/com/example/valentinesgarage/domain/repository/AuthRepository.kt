package com.example.valentinesgarage.domain.repository

import com.example.valentinesgarage.domain.model.GarageUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeSignedInUser(): Flow<GarageUser?>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
}
