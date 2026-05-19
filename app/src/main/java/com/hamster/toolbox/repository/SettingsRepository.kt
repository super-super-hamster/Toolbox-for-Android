package com.hamster.toolbox.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_MODEL_NAME = stringPreferencesKey("ai_model_name")
        val AI_BALANCE = stringPreferencesKey("ai_balance")
        val AI_TEMPERATURE = floatPreferencesKey("ai_temperature")
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AVATAR_PATH = stringPreferencesKey("user_avatar_path")
        val SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")
        val IS_CLASS_REMIND_ENABLED = booleanPreferencesKey("is_class_remind_enabled")
        val IS_DIARY_USING_PASSWORD = booleanPreferencesKey("is_diary_using_password")
    }

    val aiApiKeyFlow: Flow<String> = dataStore.data.map { it[AI_API_KEY] ?: "" }
    val aiBalanceFlow: Flow<String> = dataStore.data.map { it[AI_BALANCE] ?: "无"}
    val assistantNameFlow: Flow<String> = dataStore.data.map { it[ASSISTANT_NAME] ?: "助手"}
    val semesterStartDateFlow: Flow<String> = dataStore.data.map { it[SEMESTER_START_DATE] ?: "" }
    val userNameFlow: Flow<String> = dataStore.data.map { it[USER_NAME] ?: "无名氏" }
    val userAvatarFlow: Flow<String> = dataStore.data.map { it[USER_AVATAR_PATH] ?: "" }
    val isClassRemindEnabledFlow = dataStore.data.map { it[IS_CLASS_REMIND_ENABLED] ?: false }
    val isDiaryUsingPassword = dataStore.data.map { it[IS_DIARY_USING_PASSWORD] ?: true }

    suspend fun getAiModelName() = dataStore.data.first()[AI_MODEL_NAME] ?: "deepseek-v4-flash"
    suspend fun getAiTemperature() = dataStore.data.first()[AI_TEMPERATURE] ?: 1.0f
    suspend fun getAiApiKey() = dataStore.data.first()[AI_API_KEY] ?: ""
}