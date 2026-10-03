package com.isosic.fleetfixer.data.local

import androidx.room.TypeConverter
import com.isosic.fleetfixer.core.model.BikeComponent
import com.isosic.fleetfixer.core.model.CompletedWorkItem
import com.isosic.fleetfixer.core.model.ComponentType
import com.isosic.fleetfixer.core.model.PendingWorkItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class Converters {

    @TypeConverter
    fun fromComponents(components: List<BikeComponent>): String {
        val array = JSONArray()
        components.forEach { component ->
            array.put(
                JSONObject().apply {
                    put(KEY_TYPE, component.type.name)
                    put(KEY_NAME, component.name)
                    put(KEY_NOTES, component.notes)
                    if (component.dateAddedEpochMillis != null) {
                        put(KEY_DATE_ADDED, component.dateAddedEpochMillis)
                    } else {
                        put(KEY_DATE_ADDED, JSONObject.NULL)
                    }
                    if (component.lastServiceEpochMillis != null) {
                        put(KEY_LAST_SERVICE, component.lastServiceEpochMillis)
                    } else {
                        put(KEY_LAST_SERVICE, JSONObject.NULL)
                    }
                    put(KEY_TOTAL_DISTANCE, component.totalDistanceMeters)
                    put(KEY_DISTANCE_SINCE_SERVICE, component.distanceSinceServiceMeters)
                    put(KEY_TOTAL_MOVING_TIME, component.totalMovingTimeSeconds)
                    put(KEY_MOVING_TIME_SINCE_SERVICE, component.movingTimeSinceServiceSeconds)
                    put(KEY_PENDING_WORK, pendingWorkToJson(component.pendingWork))
                    put(KEY_COMPLETED_WORK, completedWorkToJson(component.completedWork))
                }
            )
        }
        return array.toString()
    }

    @TypeConverter
    fun toComponents(value: String): List<BikeComponent> {
        if (value.isBlank()) return emptyList()
        val array = JSONArray(value)
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                val type = runCatching {
                    ComponentType.valueOf(obj.getString(KEY_TYPE))
                }.getOrNull() ?: continue
                add(
                    BikeComponent(
                        type = type,
                        name = obj.getString(KEY_NAME),
                        notes = obj.optString(KEY_NOTES, ""),
                        dateAddedEpochMillis = obj.optionalLong(KEY_DATE_ADDED),
                        lastServiceEpochMillis = obj.optionalLong(KEY_LAST_SERVICE),
                        totalDistanceMeters = obj.optDouble(KEY_TOTAL_DISTANCE, 0.0),
                        distanceSinceServiceMeters = obj.optDouble(KEY_DISTANCE_SINCE_SERVICE, 0.0),
                        totalMovingTimeSeconds = obj.optLong(KEY_TOTAL_MOVING_TIME, 0L),
                        movingTimeSinceServiceSeconds = obj.optLong(KEY_MOVING_TIME_SINCE_SERVICE, 0L),
                        pendingWork = parsePendingWork(obj.opt(KEY_PENDING_WORK)),
                        completedWork = parseCompletedWork(obj.opt(KEY_COMPLETED_WORK))
                    )
                )
            }
        }
    }

    private fun pendingWorkToJson(items: List<PendingWorkItem>): JSONArray {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put(KEY_ID, item.id)
                    .put(KEY_DESCRIPTION, item.description)
                    .apply {
                        if (item.ruleId != null) {
                            put(KEY_RULE_ID, item.ruleId)
                        }
                    }
            )
        }
        return array
    }

    private fun completedWorkToJson(items: List<CompletedWorkItem>): JSONArray {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put(KEY_ID, item.id)
                    .put(KEY_DESCRIPTION, item.description)
                    .put(KEY_NOTES, item.notes)
                    .put(KEY_COMPLETED_AT, item.completedAtEpochMillis)
                    .apply {
                        if (item.ruleId != null) {
                            put(KEY_RULE_ID, item.ruleId)
                        }
                    }
            )
        }
        return array
    }

    private fun parsePendingWork(raw: Any?): List<PendingWorkItem> {
        return when (raw) {
            is JSONArray -> buildList {
                for (index in 0 until raw.length()) {
                    when (val entry = raw.opt(index)) {
                        is JSONObject -> {
                            val description = entry.optString(KEY_DESCRIPTION)
                                .ifBlank { entry.optString(KEY_NAME) }
                                .ifBlank { return@buildList }
                            add(
                                PendingWorkItem(
                                    id = entry.optString(KEY_ID).ifBlank { UUID.randomUUID().toString() },
                                    description = description,
                                    ruleId = entry.optString(KEY_RULE_ID).takeIf { it.isNotBlank() }
                                )
                            )
                        }
                        is String -> if (entry.isNotBlank()) {
                            add(PendingWorkItem(description = entry))
                        }
                    }
                }
            }
            is String -> if (raw.isNotBlank()) {
                listOf(PendingWorkItem(description = raw))
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }

    private fun parseCompletedWork(raw: Any?): List<CompletedWorkItem> {
        val array = raw as? JSONArray ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                val description = entry.optString(KEY_DESCRIPTION).takeIf { it.isNotBlank() }
                    ?: continue
                val completedAt = entry.optionalLong(KEY_COMPLETED_AT) ?: continue
                add(
                    CompletedWorkItem(
                        id = entry.optString(KEY_ID).ifBlank { UUID.randomUUID().toString() },
                        description = description,
                        notes = entry.optString(KEY_NOTES, ""),
                        completedAtEpochMillis = completedAt,
                        ruleId = entry.optString(KEY_RULE_ID).takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    private fun JSONObject.optionalLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return when (val value = get(key)) {
            is Long -> value
            is Number -> value.toLong()
            else -> null
        }
    }

    private companion object {
        const val KEY_TYPE = "type"
        const val KEY_NAME = "name"
        const val KEY_NOTES = "notes"
        const val KEY_DATE_ADDED = "dateAdded"
        const val KEY_LAST_SERVICE = "lastService"
        const val KEY_TOTAL_DISTANCE = "totalDistanceMeters"
        const val KEY_DISTANCE_SINCE_SERVICE = "distanceSinceServiceMeters"
        const val KEY_TOTAL_MOVING_TIME = "totalMovingTimeSeconds"
        const val KEY_MOVING_TIME_SINCE_SERVICE = "movingTimeSinceServiceSeconds"
        const val KEY_PENDING_WORK = "pendingWork"
        const val KEY_COMPLETED_WORK = "completedWork"
        const val KEY_ID = "id"
        const val KEY_DESCRIPTION = "description"
        const val KEY_RULE_ID = "ruleId"
        const val KEY_COMPLETED_AT = "completedAt"
    }
}
