package com.hamster.toolbox.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.randomStore by preferencesDataStore(name = "random")

class RandomRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        val MIN = intPreferencesKey("min")
        val MAX = intPreferencesKey("max")
    }

    val minFlow: Flow<Int> = dataStore.data.map { it[MIN] ?: 0 }
    val maxFlow: Flow<Int> = dataStore.data.map { it[MAX] ?: 10 }
}