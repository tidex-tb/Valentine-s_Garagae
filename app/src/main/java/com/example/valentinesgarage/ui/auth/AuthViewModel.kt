package com.example.valentinesgarage.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.valentinesgarage.domain.model.GarageUser
import com.example.valentinesgarage.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthState { data object Loading : AuthState; data object SignedOut : AuthState; data class SignedIn(val user: GarageUser) : AuthState; data class Error(val message: String) : AuthState }

@HiltViewModel
class AuthViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    val authState: StateFlow<AuthState> = authRepository.observeSignedInUser()
        .map { user: GarageUser? -> user?.let(AuthState::SignedIn) ?: AuthState.SignedOut }
        .catch { emit(AuthState.Error(it.message ?: "Unable to load account")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    fun signIn(email: String, password: String, onResult: (String?) -> Unit) = viewModelScope.launch {
        onResult(authRepository.signIn(email, password).exceptionOrNull()?.message)
    }
    fun signOut() = viewModelScope.launch { authRepository.signOut() }
}
