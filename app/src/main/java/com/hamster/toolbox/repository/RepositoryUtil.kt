package com.hamster.toolbox.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

suspend fun repositorySetString(dataStore : DataStore<Preferences>, string: String, key: Preferences.Key<String>) {
    dataStore.edit { it[key] = string }
}

suspend fun repositorySetBoolean(dataStore : DataStore<Preferences>, boolean: Boolean, key: Preferences.Key<Boolean>) {
    dataStore.edit { it[key] = boolean }
}

suspend fun repositorySetFloat(dataStore : DataStore<Preferences>, float: Float, key: Preferences.Key<Float>) {
    dataStore.edit { it[key] = float }
}

suspend fun repositorySetInt(dataStore : DataStore<Preferences>, int: Int, key: Preferences.Key<Int>) {
    dataStore.edit { it[key] = int }
}