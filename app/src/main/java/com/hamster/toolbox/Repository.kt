package com.hamster.toolbox

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.weatherStore by preferencesDataStore(name = "weather")
val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        val AI_MODEL_NAME = stringPreferencesKey("ai_model_name")
        val AI_BALANCE = stringPreferencesKey("ai_balance")
    }

    val aiBalanceFlow: Flow<String> = dataStore.data.map { it[AI_BALANCE] ?: "无"}

    suspend fun getAiModelName() = dataStore.data.first()[AI_MODEL_NAME] ?: "deepseek-v4-flash"

    suspend fun setAiModelName(name: String) {
        dataStore.edit { it[AI_MODEL_NAME] = name }
    }

    suspend fun setAiBalance(balance: String) {
        dataStore.edit { it[AI_BALANCE] = balance }
    }
}

class WeatherRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        val WEATHER_API_KEY = stringPreferencesKey("weather_api_key")
        val WEATHER_API_HOST = stringPreferencesKey("weather_api_host")
        val WEATHER_LAST_FETCH_TIME = longPreferencesKey("weather_last_fetch_time")
        val WEATHER_CACHED_DATA = stringPreferencesKey("weather_cached_data")
        val WEATHER_CACHED_CITY = stringPreferencesKey("weather_cached_city")
    }

    val weatherApiKeyFlow: Flow<String> = dataStore.data.map { it[WEATHER_API_KEY] ?: ""}
    val weatherApiHostFlow: Flow<String> = dataStore.data.map { it[WEATHER_API_HOST] ?: ""}
    val weatherCachedDataFlow: Flow<String> = dataStore.data.map { it[WEATHER_CACHED_DATA] ?: ""}
    val weatherCachedCityFlow: Flow<String> = dataStore.data.map { it[WEATHER_CACHED_CITY] ?: ""}

    suspend fun setApiKey(key: String) {
        dataStore.edit { it[WEATHER_API_KEY] = key }
    }

    suspend fun setApiHost(host: String) {
        dataStore.edit { it[WEATHER_API_HOST] = host }
    }

    suspend fun setLastFetchTime(time: Long) {
        dataStore.edit { it[WEATHER_LAST_FETCH_TIME] = time }
    }

    suspend fun setCachedData(data: String) {
        dataStore.edit { it[WEATHER_CACHED_DATA] = data }
    }

    suspend fun setCachedCity(city: String) {
        dataStore.edit { it[WEATHER_CACHED_CITY] = city }
    }

    suspend fun getLastFetchTime() = dataStore.data.first()[WEATHER_LAST_FETCH_TIME] ?: 0L
    suspend fun getCachedData() = dataStore.data.first()[WEATHER_CACHED_DATA] ?: ""
    suspend fun getCachedCity() = dataStore.data.first()[WEATHER_CACHED_CITY] ?: ""
}