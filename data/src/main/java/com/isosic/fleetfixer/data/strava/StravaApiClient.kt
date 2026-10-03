package com.isosic.fleetfixer.data.strava

import com.isosic.fleetfixer.core.domain.StravaAuthRepository
import com.isosic.fleetfixer.core.domain.StravaBikeRemoteSource
import com.isosic.fleetfixer.core.model.StravaActivity
import com.isosic.fleetfixer.core.model.StravaBike
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Instant

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

    override suspend fun fetchActivities(
        afterEpochSeconds: Long?
    ): Result<List<StravaActivity>> = withContext(Dispatchers.IO) {
        runCatching {
            val accessToken = authRepository.getValidAccessToken()
            val activities = mutableListOf<StravaActivity>()
            var page = 1
            while (page <= MAX_ACTIVITY_PAGES) {
                val batch = fetchActivitiesPage(
                    accessToken = accessToken,
                    page = page,
                    afterEpochSeconds = afterEpochSeconds
                )
                activities += batch
                if (batch.size < ACTIVITIES_PER_PAGE) break
                page++
            }
            activities
        }
    }

    private fun fetchActivitiesPage(
        accessToken: String,
        page: Int,
        afterEpochSeconds: Long?
    ): List<StravaActivity> {
        val urlBuilder = "${StravaConfig.API_BASE_URL}/athlete/activities".toHttpUrl()
            .newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("per_page", ACTIVITIES_PER_PAGE.toString())
        if (afterEpochSeconds != null) {
            urlBuilder.addQueryParameter("after", afterEpochSeconds.toString())
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch Strava activities: HTTP ${response.code} $payload")
            }
            return parseActivities(payload)
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

    private fun parseActivities(payload: String): List<StravaActivity> {
        val array = JSONArray(payload)
        return buildList {
            for (index in 0 until array.length()) {
                val activity = array.optJSONObject(index) ?: continue
                val id = activity.optLong("id", -1L).takeIf { it > 0L } ?: continue
                val startDate = activity.optString("start_date").takeIf { it.isNotBlank() }
                    ?: continue
                val startEpochSeconds = runCatching {
                    Instant.parse(startDate).epochSecond
                }.getOrNull() ?: continue
                val gearId = activity.optString("gear_id").takeIf { it.isNotBlank() }
                add(
                    StravaActivity(
                        id = id,
                        distanceMeters = activity.optDouble("distance", 0.0),
                        movingTimeSeconds = activity.optLong("moving_time", 0L),
                        startDateEpochSeconds = startEpochSeconds,
                        gearId = gearId
                    )
                )
            }
        }
    }

    private companion object {
        const val ACTIVITIES_PER_PAGE = 200
        const val MAX_ACTIVITY_PAGES = 50
    }
}
