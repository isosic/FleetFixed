package com.isosic.fleetfixer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.isosic.fleetfixer.core.domain.SelectedBikeStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.selectedBikeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "selected_bike_prefs"
)

class SelectedBikeStoreImpl(private val context: Context) : SelectedBikeStore {

    override val selectedBikeId: Flow<String?> = context.selectedBikeDataStore.data.map { prefs ->
        prefs[KEY_SELECTED_BIKE_ID]
    }

    override suspend fun select(bikeId: String) {
        context.selectedBikeDataStore.edit { prefs ->
            prefs[KEY_SELECTED_BIKE_ID] = bikeId
        }
    }

    override suspend fun clear() {
        context.selectedBikeDataStore.edit { prefs ->
            prefs.remove(KEY_SELECTED_BIKE_ID)
        }
    }

    private companion object {
        val KEY_SELECTED_BIKE_ID = stringPreferencesKey("selected_bike_id")
    }
}
