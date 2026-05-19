package com.hamster.toolbox.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.debugStore by preferencesDataStore(name = "debug")

class DebugRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        val SHOW_PACKAGE_NAME = booleanPreferencesKey("show_package_name")
    }

    val showPackageNameFlow: Flow<Boolean> = dataStore.data.map { it[SHOW_PACKAGE_NAME] ?: false }
}