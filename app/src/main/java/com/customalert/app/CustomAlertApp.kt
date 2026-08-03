package com.customalert.app

import android.app.Application
import com.customalert.app.data.AlertRepository
import com.customalert.app.data.AppDatabase
import com.customalert.app.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CustomAlertApp : Application() {
    lateinit var repository: AlertRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        val preferences = UserPreferences(this)
        repository = AlertRepository(this, db, preferences)
        appScope.launch {
            repository.ensureBuiltinSounds()
        }
    }
}
