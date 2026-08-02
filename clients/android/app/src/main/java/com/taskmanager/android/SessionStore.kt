package com.taskmanager.android

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("tm_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().putString("token", value).apply()

    var baseUrl: String
        get() = prefs.getString("baseUrl", ApiConfig.DEFAULT_BASE_URL) ?: ApiConfig.DEFAULT_BASE_URL
        set(value) = prefs.edit().putString("baseUrl", value).apply()

    fun clear() {
        prefs.edit().remove("token").apply()
    }
}
