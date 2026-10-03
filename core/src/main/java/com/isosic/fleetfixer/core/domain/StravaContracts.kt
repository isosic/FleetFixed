package com.isosic.fleetfixer.core.domain

import android.content.Context
import android.net.Uri
import com.isosic.fleetfixer.core.model.StravaAuthEvent
import com.isosic.fleetfixer.core.model.StravaActivity
import com.isosic.fleetfixer.core.model.StravaBike
import com.isosic.fleetfixer.core.model.StravaTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface StravaAuthRepository {
    val authEvents: SharedFlow<StravaAuthEvent>
    val isConnected: Flow<Boolean>
    fun hasCredentials(): Boolean
    fun redirectHost(): String
    fun launchAuthorization(context: Context)
    fun isCallbackUri(uri: Uri?): Boolean
    suspend fun handleCallbackIntent(uri: Uri): Boolean
    suspend fun onHostResumedWithoutFreshCallback()
    suspend fun disconnect(): Result<Unit>
    suspend fun getValidAccessToken(): String
}

interface StravaBikeRemoteSource {
    suspend fun fetchAthleteBikes(): Result<List<StravaBike>>
    suspend fun fetchActivities(afterEpochSeconds: Long? = null): Result<List<StravaActivity>>
}

interface StravaTokenStore {
    val isConnectedFlow: Flow<Boolean>
    suspend fun getTokens(): StravaTokens?
    suspend fun saveTokens(tokens: StravaTokens)
    suspend fun clearTokens()
    suspend fun getLastBikeSyncEpochMillis(): Long
    suspend fun setLastBikeSyncEpochMillis(epochMillis: Long)
}
