package com.isosic.fleetfixer.core.domain

import kotlinx.coroutines.flow.Flow

interface AuthTokenStore {
    val tokenFlow: Flow<String?>
    suspend fun getToken(): String?
    suspend fun hasToken(): Boolean
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}
