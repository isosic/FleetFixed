package com.isosic.fleetfixer.data.strava

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.isosic.fleetfixer.core.domain.StravaAuthRepository
import com.isosic.fleetfixer.core.domain.StravaTokenStore
import com.isosic.fleetfixer.core.model.StravaAuthEvent
import com.isosic.fleetfixer.core.model.StravaTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class StravaAuthClient(
    private val tokenStore: StravaTokenStore,
    private val httpClient: OkHttpClient = OkHttpClient()
) : StravaAuthRepository {

    private val authorizationInProgress = AtomicBoolean(false)

    private val _authEvents = MutableSharedFlow<StravaAuthEvent>(extraBufferCapacity = 1)
    override val authEvents: SharedFlow<StravaAuthEvent> = _authEvents.asSharedFlow()

    override val isConnected: Flow<Boolean> = tokenStore.isConnectedFlow

    override fun hasCredentials(): Boolean = StravaConfig.hasCredentials()

    override fun redirectHost(): String = StravaConfig.REDIRECT_HOST

    fun buildAuthorizeUri(): Uri {
        require(StravaConfig.hasCredentials()) {
            "Add strava.client.id and strava.client.secret to local.properties"
        }
        return StravaConfig.AUTHORIZE_URL.toUri()
            .buildUpon()
            .appendQueryParameter("client_id", StravaConfig.clientId)
            .appendQueryParameter("redirect_uri", StravaConfig.REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", StravaConfig.SCOPES)
            .build()
    }

    override fun launchAuthorization(context: Context) {
        authorizationInProgress.set(true)
        context.startActivity(Intent(Intent.ACTION_VIEW, buildAuthorizeUri()))
    }

    override fun isCallbackUri(uri: Uri?): Boolean {
        if (uri == null) return false
        return uri.scheme == StravaConfig.REDIRECT_SCHEME &&
            uri.host == StravaConfig.REDIRECT_HOST
    }

    override suspend fun handleCallbackIntent(uri: Uri): Boolean {
        if (!isCallbackUri(uri)) return false
        Log.i(TAG, "Strava callback received: $uri")
        authorizationInProgress.set(false)

        val error = uri.getQueryParameter("error")
        if (error != null) {
            _authEvents.emit(StravaAuthEvent.Failed("Strava authorization denied: $error"))
            return true
        }

        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            _authEvents.emit(StravaAuthEvent.Failed("Missing authorization code from Strava"))
            return true
        }

        val grantedScope = uri.getQueryParameter("scope")
        exchangeAuthorizationCode(code, grantedScope)
            .onSuccess {
                Log.i(TAG, "Strava token exchange succeeded")
                _authEvents.emit(StravaAuthEvent.Connected)
            }
            .onFailure { exchangeError ->
                Log.e(TAG, "Strava token exchange failed", exchangeError)
                _authEvents.emit(
                    StravaAuthEvent.Failed(
                        exchangeError.localizedMessage ?: "Strava connection failed"
                    )
                )
            }
        return true
    }

    override suspend fun onHostResumedWithoutFreshCallback() {
        if (authorizationInProgress.compareAndSet(true, false)) {
            Log.w(TAG, "Returned to app without Strava callback deep link")
            _authEvents.emit(StravaAuthEvent.Cancelled)
        }
    }

    suspend fun exchangeAuthorizationCode(
        code: String,
        grantedScope: String? = null
    ): Result<StravaTokens> = withContext(Dispatchers.IO) {
        runCatching {
            val body = FormBody.Builder()
                .add("client_id", StravaConfig.clientId)
                .add("client_secret", StravaConfig.clientSecret)
                .add("code", code)
                .add("grant_type", "authorization_code")
                .build()
            val tokens = requestTokens(body).copy(scope = grantedScope)
            tokenStore.saveTokens(tokens)
            tokens
        }
    }

    override suspend fun getValidAccessToken(): String {
        val tokens = tokenStore.getTokens()
            ?: throw IllegalStateException("Strava is not connected")
        if (!tokens.isExpired()) {
            return tokens.accessToken
        }
        return refreshTokens(tokens.refreshToken).getOrThrow().accessToken
    }

    suspend fun refreshTokens(refreshToken: String): Result<StravaTokens> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = FormBody.Builder()
                    .add("client_id", StravaConfig.clientId)
                    .add("client_secret", StravaConfig.clientSecret)
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .build()
                val existing = tokenStore.getTokens()
                val tokens = requestTokens(body).copy(
                    athleteId = existing?.athleteId,
                    scope = existing?.scope
                )
                tokenStore.saveTokens(tokens)
                tokens
            }
        }

    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val tokens = tokenStore.getTokens()
            if (tokens != null) {
                runCatching { deauthorize(tokens.accessToken) }
            }
            tokenStore.clearTokens()
        }
    }

    private fun deauthorize(accessToken: String) {
        val body = FormBody.Builder()
            .add("access_token", accessToken)
            .build()
        val request = Request.Builder()
            .url(StravaConfig.DEAUTHORIZE_URL)
            .post(body)
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Strava deauthorize failed: HTTP ${response.code}")
            }
        }
    }

    private fun requestTokens(body: FormBody): StravaTokens {
        val request = Request.Builder()
            .url(StravaConfig.TOKEN_URL)
            .post(body)
            .build()
        httpClient.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Strava token request failed: HTTP ${response.code} $payload")
            }
            val json = JSONObject(payload)
            val athleteId = json.optJSONObject("athlete")?.optLong("id")
            return StravaTokens(
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                expiresAtEpochSeconds = json.getLong("expires_at"),
                athleteId = athleteId?.takeIf { it != 0L },
                scope = json.optString("scope").takeIf { it.isNotBlank() }
            )
        }
    }

    private companion object {
        const val TAG = "StravaAuth"
    }
}
