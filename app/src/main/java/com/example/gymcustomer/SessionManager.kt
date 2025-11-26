package com.example.gymcustomer

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private var preferences: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_UID = "uid"
        private const val KEY_FULL_NAME = "full_name"
    }

    fun saveLoginSession(uid: String, fullName: String) {
        val editor = preferences.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_UID, uid)
        editor.putString(KEY_FULL_NAME, fullName)
        editor.apply()
    }

    fun clearLoginSession() {
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
    }

    fun isLoggedIn(): Boolean = preferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUid(): String? = preferences.getString(KEY_UID, null)

    fun getFullName(): String? = preferences.getString(KEY_FULL_NAME, null)
}