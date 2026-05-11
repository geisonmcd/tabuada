package com.geison.tabuada.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

object PracticeStorage {
    private const val PREFS_NAME = "tabuada_prefs"
    private const val STATE_KEY = "app_state"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(context: Context): AppState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(STATE_KEY, null) ?: return AppState()
        return runCatching {
            json.decodeFromJsonElement<AppState>(json.parseToJsonElement(raw))
        }.getOrDefault(AppState())
    }

    fun save(context: Context, state: AppState) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(STATE_KEY, json.encodeToString(state))
            .apply()
    }
}
