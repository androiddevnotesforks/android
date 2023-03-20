/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package fusion.ai.datasource.cache.datastore

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fusion.ai.BuildConfig
import fusion.ai.billing.Plan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val app: Application
) {
    private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "preference")

    suspend fun updateApiKey(apiKey: String) {
        app.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }

    val getApiKey: Flow<String?> = app.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preference ->
            preference[API_KEY]
        }

    suspend fun setUserId(userId: String) {
        app.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
        }
    }

    val getUserId: Flow<String?> = app.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preference ->
            preference[USER_ID]
        }

    suspend fun updatePremiumStatus(status: Boolean, plan: Plan) {
        app.dataStore.edit { preferences ->
            preferences[IS_PREMIUM] = status
            preferences[CURRENT_PLAN] = plan.token
        }
    }

    val getCurrentPlan: Flow<Plan> = app.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preference ->
            preference[CURRENT_PLAN]?.let {
                when (it) {
                    Plan.Monthly.token -> Plan.Monthly
                    Plan.ThreeMonthly.token -> Plan.ThreeMonthly
                    else -> Plan.Trial
                }
            } ?: Plan.Trial
        }

    suspend fun updateStreamResponse(value: Boolean) {
        app.dataStore.edit { preferences ->
            preferences[STREAM_RESPONSE] = value
        }
    }

    val streamResponse: Flow<Boolean> = app.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preference ->
            preference[STREAM_RESPONSE] ?: false
        }

    suspend fun updateInitialMessageShown() {
        app.dataStore.edit { preferences ->
            preferences[INITIAL_MESSAGE_SHOWN] = initialMessageId
        }
    }

    val initialMessageShown: Flow<Boolean?> = app.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preference ->
            initialMessageId == preference[INITIAL_MESSAGE_SHOWN]
        }

    companion object {
        private val USER_ID =
            stringPreferencesKey("user_id")
        private val CURRENT_PLAN =
            stringPreferencesKey("premium_plan")
        private val IS_PREMIUM =
            booleanPreferencesKey("is_premium")
        private val API_KEY =
            stringPreferencesKey("api_key")
        private val STREAM_RESPONSE =
            booleanPreferencesKey("stream_response")
        private val INITIAL_MESSAGE_SHOWN =
            intPreferencesKey("initial_message_shown")

        // Update the key to some random value when we want to show a new initial message (for eg: updated changelog)
        private const val initialMessageId = BuildConfig.VERSION_CODE
    }
}
