package com.isosic.fleetfixer.data.local

import androidx.room.TypeConverter
import com.isosic.fleetfixer.core.model.BikeComponent
import com.isosic.fleetfixer.core.model.ComponentType
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromComponents(components: List<BikeComponent>): String {
        val array = JSONArray()
        components.forEach { component ->
            array.put(
                JSONObject()
                    .put(KEY_TYPE, component.type.name)
                    .put(KEY_NAME, component.name)
                    .put(KEY_NOTES, component.notes)
                    .put(KEY_DATE_ADDED, component.dateAddedEpochMillis)
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
                        dateAddedEpochMillis = obj.getLong(KEY_DATE_ADDED)
                    )
                )
            }
        }
    }

    private companion object {
        const val KEY_TYPE = "type"
        const val KEY_NAME = "name"
        const val KEY_NOTES = "notes"
        const val KEY_DATE_ADDED = "dateAdded"
    }
}
