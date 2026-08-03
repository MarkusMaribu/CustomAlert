package com.customalert.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _preferReplace = MutableStateFlow(prefs.getBoolean(KEY_PREFER_REPLACE, true))
    val preferReplace: StateFlow<Boolean> = _preferReplace.asStateFlow()

    private val _onboardingDone = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    private val _monitoringEnabled = MutableStateFlow(prefs.getBoolean(KEY_MONITORING, true))
    val monitoringEnabled: StateFlow<Boolean> = _monitoringEnabled.asStateFlow()

    fun setPreferReplace(value: Boolean) {
        prefs.edit().putBoolean(KEY_PREFER_REPLACE, value).apply()
        _preferReplace.value = value
    }

    fun setOnboardingDone(value: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()
        _onboardingDone.value = value
    }

    fun setMonitoringEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING, value).apply()
        _monitoringEnabled.value = value
    }

    fun isPreferReplace(): Boolean = prefs.getBoolean(KEY_PREFER_REPLACE, true)
    fun isMonitoringEnabled(): Boolean = prefs.getBoolean(KEY_MONITORING, true)
    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    companion object {
        private const val PREFS_NAME = "custom_alert_prefs"
        private const val KEY_PREFER_REPLACE = "prefer_replace"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_MONITORING = "monitoring_enabled"
    }
}
