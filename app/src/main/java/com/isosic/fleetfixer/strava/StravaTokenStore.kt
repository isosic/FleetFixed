package com.isosic.fleetfixer.strava

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.stravaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "strava_prefs"
)

class StravaTokenStore(private val context: Context) {

    val tokensFlow: Flow<StravaTokens?> = context.stravaDataStore.data.map { prefs ->
        prefs.toTokens()
    }

    val isConnectedFlow: Flow<Boolean> = tokensFlow.map { tokens ->
        !tokens?.refreshToken.isNullOrBlank()
    }

    suspend fun getTokens(): StravaTokens? = tokensFlow.first()

    suspend fun isConnected(): Boolean = isConnectedFlow.first()

    suspend fun saveTokens(tokens: StravaTokens) {
        context.stravaDataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = tokens.accessToken
            prefs[KEY_REFRESH_TOKEN] = tokens.refreshToken
            prefs[KEY_EXPIRES_AT] = tokens.expiresAtEpochSeconds
            tokens.athleteId?.let { prefs[KEY_ATHLETE_ID] = it }
                ?: prefs.remove(KEY_ATHLETE_ID)
            tokens.scope?.let { prefs[KEY_SCOPE] = it }
                ?: prefs.remove(KEY_SCOPE)
        }
    }

    suspend fun clearTokens() {
        context.stravaDataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_EXPIRES_AT)
            prefs.remove(KEY_ATHLETE_ID)
            prefs.remove(KEY_SCOPE)
        }
    }

    private fun Preferences.toTokens(): StravaTokens? {
        val accessToken = this[KEY_ACCESS_TOKEN] ?: return null
        val refreshToken = this[KEY_REFRESH_TOKEN] ?: return null
        val expiresAt = this[KEY_EXPIRES_AT] ?: return null
        if (accessToken.isBlank() || refreshToken.isBlank()) return null
        return StravaTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = expiresAt,
            athleteId = this[KEY_ATHLETE_ID],
            scope = this[KEY_SCOPE]
        )
    }

    private companion object {
        val KEY_ACCESS_TOKEN = stringPreferencesKey("strava_access_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("strava_refresh_token")
        val KEY_EXPIRES_AT = longPreferencesKey("strava_expires_at")
        val KEY_ATHLETE_ID = longPreferencesKey("strava_athlete_id")
        val KEY_SCOPE = stringPreferencesKey("strava_scope")
    }
}
