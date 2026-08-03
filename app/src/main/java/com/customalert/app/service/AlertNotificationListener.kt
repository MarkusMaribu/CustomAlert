package com.customalert.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.customalert.app.CustomAlertApp
import com.customalert.app.R
import com.customalert.app.data.RuleScope
import com.customalert.app.domain.NotificationContent
import com.customalert.app.domain.RuleMatcher
import com.customalert.app.domain.SoundResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlertNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var soundPlayer: SoundPlayer

    override fun onCreate() {
        super.onCreate()
        soundPlayer = SoundPlayer(this)
        ensureMirrorChannel()
    }

    override fun onDestroy() {
        scope.cancel()
        soundPlayer.release()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val app = application as CustomAlertApp
        if (app.repository.preferences.isMonitoringEnabled()) {
            MonitorForegroundService.start(this)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName == packageName) return
        if (sbn.isOngoing) return

        val app = application as CustomAlertApp
        if (!app.repository.preferences.isMonitoringEnabled()) return

        val notification = sbn.notification ?: return
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = buildString {
            extras?.getCharSequence(Notification.EXTRA_TEXT)?.let { append(it) }
            val big = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            if (!big.isNullOrBlank() && !contains(big)) {
                if (isNotEmpty()) append(' ')
                append(big)
            }
        }

        val content = NotificationContent(
            packageName = sbn.packageName,
            title = title,
            text = text
        )

        scope.launch {
            handleNotification(app, sbn, content)
        }
    }

    private suspend fun handleNotification(
        app: CustomAlertApp,
        sbn: StatusBarNotification,
        content: NotificationContent
    ) {
        val mapping = app.repository.getAppMapping(content.packageName)
        val candidates = app.repository.getEnabledRulesForPackage(content.packageName)
            .filter { rule ->
                mapping?.enabled != false || rule.scope == RuleScope.GLOBAL
            }

        val effectiveMapping = if (mapping?.enabled == false) null else mapping
        val match = RuleMatcher.resolve(content, candidates, effectiveMapping) ?: return
        val asset = app.repository.getSound(match.soundId) ?: return
        val uri = SoundResolver.resolveUri(this, asset) ?: return

        val preferReplace = app.repository.preferences.isPreferReplace()
        if (preferReplace && canSafelyReplace(sbn)) {
            tryReplace(sbn, content)
        }
        // Always play via MediaPlayer so the chosen sound is heard even if the
        // notification channel cannot use a per-notification custom tone.
        soundPlayer.play(uri)
    }

    private fun canSafelyReplace(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        if (sbn.isOngoing) return false
        if ((notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) return false
        if ((notification.flags and Notification.FLAG_NO_CLEAR) != 0) return false
        if (notification.category == Notification.CATEGORY_CALL) return false
        if (notification.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true) return false
        if (notification.actions?.any { action ->
                action.remoteInputs?.isNotEmpty() == true
            } == true
        ) {
            return false
        }
        return true
    }

    private fun tryReplace(sbn: StatusBarNotification, content: NotificationContent) {
        try {
            cancelNotification(sbn.key)
            val manager = getSystemService(NotificationManager::class.java)
            val launchIntent = packageManager.getLaunchIntentForPackage(content.packageName)
            val contentIntent = launchIntent?.let {
                PendingIntent.getActivity(
                    this,
                    sbn.id,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            val appLabel = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(content.packageName, 0)
                ).toString()
            } catch (_: Exception) {
                content.packageName
            }

            val builder = NotificationCompat.Builder(this, MIRROR_CHANNEL_ID)
                .setContentTitle(content.title.ifBlank { appLabel })
                .setContentText(content.text.ifBlank { "Notification" })
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSubText(appLabel)
            contentIntent?.let { builder.setContentIntent(it) }

            manager.notify(
                MIRROR_TAG,
                sbn.id xor content.packageName.hashCode(),
                builder.build()
            )
        } catch (_: Exception) {
            // Fallback is simply playing the sound without canceling/reposting.
        }
    }

    private fun ensureMirrorChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(MIRROR_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            MIRROR_CHANNEL_ID,
            getString(R.string.mirror_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.mirror_channel_desc)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val MIRROR_CHANNEL_ID = "custom_alert_mirror"
        private const val MIRROR_TAG = "custom_alert_mirror"
    }
}
