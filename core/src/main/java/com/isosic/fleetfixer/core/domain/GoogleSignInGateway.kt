package com.isosic.fleetfixer.core.domain

import android.content.Context

interface GoogleSignInGateway {
    suspend fun signIn(activityContext: Context, serverClientId: String): Result<String>
    suspend fun signOut()
}
