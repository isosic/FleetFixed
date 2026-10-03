package com.isosic.fleetfixer.data.strava

import com.isosic.fleetfixer.core.domain.StravaAuthRepository
import com.isosic.fleetfixer.core.domain.StravaBikeRemoteSource
import com.isosic.fleetfixer.core.model.StravaBike
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class StravaApiClient(
    private val authRepository: StravaAuthRepository,
    private val httpClient: OkHttpClient = OkHttpClient()
) : StravaBikeRemoteSource {

    override suspend fun fetchAthleteBikes(): Result<List<StravaBike>> = withContext(Dispatchers.IO) {
        runCatching {
            val accessToken = authRepository.getValidAccessToken()
            val request = Request.Builder()
                .url("${StravaConfig.API_BASE_URL}/athlete")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Failed to fetch Strava athlete: HTTP ${response.code} $payload")
                }
                parseBikes(payload)
            }
        }
    }

    private fun parseBikes(payload: String): List<StravaBike> {
        val bikesJson = JSONObject(payload).optJSONArray("bikes") ?: return emptyList()
        return buildList {
            for (index in 0 until bikesJson.length()) {
                val bike = bikesJson.optJSONObject(index) ?: continue
                val id = bike.optString("id").takeIf { it.isNotBlank() } ?: continue
                val name = bike.optString("name").ifBlank { "Strava bike" }
                add(
                    StravaBike(
                        id = id,
                        name = name,
                        primary = bike.optBoolean("primary"),
                        distanceMeters = bike.optDouble("distance", 0.0)
                    )
                )
            }
        }
    }
}
