package com.example.mystockwatch.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mystockwatch.domain.model.ChartLibrary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "chart_prefs")

class ChartPrefs(private val context: Context) {

    private val KEY_LIBRARY = stringPreferencesKey("chart_library")

    val chartLibrary: Flow<ChartLibrary> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_LIBRARY] ?: ChartLibrary.COMPOSE.name
        ChartLibrary.valueOf(name)
    }

    suspend fun setChartLibrary(lib: ChartLibrary) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LIBRARY] = lib.name
        }
    }
}
