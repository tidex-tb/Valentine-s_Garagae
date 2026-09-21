package com.example.valentinesgarage.data.auth

import com.example.valentinesgarage.domain.model.GarageUser
import com.example.valentinesgarage.domain.model.UserRole
import com.example.valentinesgarage.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
    override fun observeSignedInUser(): Flow<GarageUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.mapLatest { firebaseUser ->
        firebaseUser ?: return@mapLatest null
        val profile = firestore.collection(USERS).document(firebaseUser.uid).get().await()
        GarageUser(
            id = firebaseUser.uid,
            displayName = profile.getString("displayName") ?: firebaseUser.displayName ?: firebaseUser.email.orEmpty(),
            email = firebaseUser.email.orEmpty(),
            role = runCatching { UserRole.valueOf(profile.getString("role") ?: "MECHANIC") }.getOrDefault(UserRole.MECHANIC)
        )
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun signOut() = auth.signOut()

    private companion object { const val USERS = "users" }
}
