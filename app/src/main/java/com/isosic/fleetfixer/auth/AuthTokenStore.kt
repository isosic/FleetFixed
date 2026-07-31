package com.isosic.fleetfixer.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_prefs"
)

class AuthTokenStore(private val context: Context) {

    val tokenFlow: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[KEY_GOOGLE_ID_TOKEN]
    }

    suspend fun getToken(): String? = tokenFlow.first()

    suspend fun hasToken(): Boolean = !getToken().isNullOrBlank()

    suspend fun saveToken(token: String) {
        context.authDataStore.edit { prefs ->
            prefs[KEY_GOOGLE_ID_TOKEN] = token
        }
    }

    suspend fun clearToken() {
        context.authDataStore.edit { prefs ->
            prefs.remove(KEY_GOOGLE_ID_TOKEN)
        }
    }

    private companion object {
        val KEY_GOOGLE_ID_TOKEN = stringPreferencesKey("google_id_token")
    }
}
