package com.isosic.fleetfixer.screens.login

import android.app.Activity
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.isosic.fleetfixer.auth.AuthTokenStore
import com.isosic.fleetfixer.auth.GoogleAuthClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(
    private val authTokenStore: AuthTokenStore,
    private val googleAuthClient: GoogleAuthClient,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signInWithGoogle(
        activity: Activity,
        serverClientId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            googleAuthClient.signIn(activity, serverClientId)
                .onSuccess { idToken ->
                    runCatching {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        firebaseAuth.signInWithCredential(credential).await()
                        authTokenStore.saveToken(idToken)
                    }.onSuccess {
                        _uiState.update { it.copy(isLoading = false) }
                        onSuccess()
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.localizedMessage ?: "Firebase sign-in failed"
                            )
                        }
                    }
                }
                .onFailure { error ->
                    val message = when (error) {
                        is GetCredentialCancellationException -> null
                        else -> error.localizedMessage ?: "Google sign-in failed"
                    }
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = message)
                    }
                }
        }
    }
}
