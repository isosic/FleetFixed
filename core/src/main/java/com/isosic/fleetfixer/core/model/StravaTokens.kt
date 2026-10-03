package com.isosic.fleetfixer.core.model

data class StravaTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val athleteId: Long? = null,
    val scope: String? = null
) {
    fun isExpired(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean =
        expiresAtEpochSeconds <= nowEpochSeconds + EXPIRY_SKEW_SECONDS

    companion object {
        private const val EXPIRY_SKEW_SECONDS = 60L
    }
}
