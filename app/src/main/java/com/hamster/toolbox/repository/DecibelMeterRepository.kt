package com.hamster.toolbox.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.decibelMeterStore by preferencesDataStore(name = "decibel_meter")

class DecibelMeterRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        val OFFSET = floatPreferencesKey("offset")
    }

    val offsetFlow: Flow<Float> = dataStore.data.map { it[OFFSET] ?: 0f }
}