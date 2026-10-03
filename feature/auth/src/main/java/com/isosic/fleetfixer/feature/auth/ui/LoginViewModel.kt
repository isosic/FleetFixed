package com.isosic.fleetfixer.feature.auth.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isosic.fleetfixer.core.domain.AppAuth
import com.isosic.fleetfixer.core.domain.AuthTokenStore
import com.isosic.fleetfixer.core.domain.GoogleSignInGateway
import com.isosic.fleetfixer.core.domain.SignInCancelledException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(
    private val authTokenStore: AuthTokenStore,
    private val googleSignInGateway: GoogleSignInGateway,
    private val appAuth: AppAuth
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

            googleSignInGateway.signIn(activity, serverClientId)
                .onSuccess { idToken ->
                    runCatching {
                        appAuth.signInWithGoogleIdToken(idToken)
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
                        is SignInCancelledException -> null
                        else -> error.localizedMessage ?: "Google sign-in failed"
                    }
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = message)
                    }
                }
        }
    }
}
