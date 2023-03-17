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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fusion.ai.billing.Plan
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

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
            preferences[CURRENT_PLAN] = plan.id
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
                    Plan.Monthly.id -> Plan.Monthly
                    Plan.Lifetime.id -> Plan.Lifetime
                    else -> Plan.Trial
                }
            } ?: Plan.Trial
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
    }
}
