package com.isosic.fleetfixer.core.domain

interface AppAuth {
    val currentUserId: String?
    fun hasCurrentUser(): Boolean
    suspend fun signInWithGoogleIdToken(idToken: String)
    fun signOut()
}
