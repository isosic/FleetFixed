package com.isosic.fleetfixer.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.isosic.fleetfixer.core.domain.AppAuth
import kotlinx.coroutines.tasks.await

class FirebaseAppAuth(
    private val firebaseAuth: FirebaseAuth
) : AppAuth {

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override fun hasCurrentUser(): Boolean = firebaseAuth.currentUser != null

    override suspend fun signInWithGoogleIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}
