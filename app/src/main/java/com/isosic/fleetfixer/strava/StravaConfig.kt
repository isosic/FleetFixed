package com.isosic.fleetfixer.strava

import com.isosic.fleetfixer.BuildConfig

object StravaConfig {
    const val REDIRECT_SCHEME = "fleetfixer"
    const val REDIRECT_HOST = "com.isosic.fleetfixer"

    /**
     * Strava mobile OAuth requires: `{scheme}://{Authorization Callback Domain}`
     * Callback domain in Strava settings must match [REDIRECT_HOST].
     */
    const val REDIRECT_URI = "$REDIRECT_SCHEME://$REDIRECT_HOST"
    const val SCOPES = "read,activity:read_all,profile:read_all"
    const val AUTHORIZE_URL = "https://www.strava.com/oauth/mobile/authorize"
    const val TOKEN_URL = "https://www.strava.com/oauth/token"
    const val DEAUTHORIZE_URL = "https://www.strava.com/oauth/deauthorize"
    const val API_BASE_URL = "https://www.strava.com/api/v3"

    val clientId: String get() = BuildConfig.STRAVA_CLIENT_ID
    val clientSecret: String get() = BuildConfig.STRAVA_CLIENT_SECRET

    fun hasCredentials(): Boolean =
        clientId.isNotBlank() && clientSecret.isNotBlank()
}
