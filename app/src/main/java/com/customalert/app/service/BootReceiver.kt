package com.customalert.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.customalert.app.CustomAlertApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? CustomAlertApp ?: return
        if (app.repository.preferences.isMonitoringEnabled()) {
            MonitorForegroundService.start(context)
        }
    }
}
