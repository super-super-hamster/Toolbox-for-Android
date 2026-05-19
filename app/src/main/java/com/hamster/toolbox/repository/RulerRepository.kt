package com.hamster.toolbox.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.rulerStore by preferencesDataStore(name = "ruler")

class RulerRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        val ZOOM_FACTOR = floatPreferencesKey("zoom_factor")
    }

    val zoomFactorFlow: Flow<Float> = dataStore.data.map { it[ZOOM_FACTOR] ?: 1f }
}