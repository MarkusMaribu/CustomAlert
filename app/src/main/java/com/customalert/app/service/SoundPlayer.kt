package com.customalert.app.service

import android.app.NotificationManager
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.net.Uri
import android.os.PowerManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Plays alert tones through [SoundPool] with [AudioAttributes.USAGE_NOTIFICATION]
 * so volume follows the notification stream and DND can mute them.
 */
class SoundPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(NotificationManager::class.java)
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(audioAttributes)
        .build()

    private val loadedSounds = ConcurrentHashMap<String, Int>()
    private val pendingPlay = ConcurrentHashMap.newKeySet<Int>()
    private val openDescriptors = ConcurrentHashMap<Int, AssetFileDescriptor>()
    private val activeStreams = ConcurrentHashMap.newKeySet<Int>()

    private val wakeLock: PowerManager.WakeLock =
        (appContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CustomAlert:SoundPlay")
            .apply { setReferenceCounted(false) }

    @Volatile
    private var released = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            closeDescriptor(sampleId)
            if (status != 0) {
                pendingPlay.remove(sampleId)
                return@setOnLoadCompleteListener
            }
            if (pendingPlay.remove(sampleId)) {
                playLoaded(sampleId)
            }
        }
    }

    /**
     * @param respectInterruptionFilter when true (default), skip playback under DND /
     * silent ringer. Preview can pass false to audition tones anyway.
     */
    @Synchronized
    fun play(uri: Uri, respectInterruptionFilter: Boolean = true) {
        if (released) return
        if (respectInterruptionFilter && !canPlayNotificationSound()) return

        val key = uri.toString()
        val existing = loadedSounds[key]
        if (existing != null && existing != 0) {
            playLoaded(existing)
            return
        }

        try {
            val afd = appContext.contentResolver.openAssetFileDescriptor(uri, "r") ?: return
            val soundId = soundPool.load(afd.fileDescriptor, afd.startOffset, afd.length, 1)
            if (soundId == 0) {
                try {
                    afd.close()
                } catch (_: Exception) {
                }
                return
            }
            openDescriptors[soundId] = afd
            loadedSounds[key] = soundId
            pendingPlay.add(soundId)
        } catch (_: Exception) {
            // Ignore unreadable / unsupported sources.
        }
    }

    @Synchronized
    fun stop() {
        activeStreams.forEach { streamId ->
            try {
                soundPool.stop(streamId)
            } catch (_: Exception) {
            }
        }
        activeStreams.clear()
        pendingPlay.clear()
        releaseWakeLock()
    }

    @Synchronized
    fun release() {
        if (released) return
        stop()
        loadedSounds.clear()
        openDescriptors.keys.toList().forEach { closeDescriptor(it) }
        try {
            soundPool.release()
        } catch (_: Exception) {
        }
        released = true
    }

    private fun playLoaded(soundId: Int) {
        if (released) return
        try {
            wakeLock.acquire(10_000L)
            // Volume scalars are relative; stream level follows USAGE_NOTIFICATION.
            val streamId = soundPool.play(
                /* soundID = */ soundId,
                /* leftVolume = */ 1f,
                /* rightVolume = */ 1f,
                /* priority = */ 1,
                /* loop = */ 0,
                /* rate = */ 1f
            )
            if (streamId != 0) {
                activeStreams.add(streamId)
            } else {
                releaseWakeLock()
            }
        } catch (_: Exception) {
            releaseWakeLock()
        }
    }

    private fun canPlayNotificationSound(): Boolean {
        val filter = notificationManager?.currentInterruptionFilter
            ?: NotificationManager.INTERRUPTION_FILTER_ALL
        val dndAllowsNotifications = when (filter) {
            NotificationManager.INTERRUPTION_FILTER_ALL,
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> true
            else -> false
        }
        if (!dndAllowsNotifications) return false

        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> true
            else -> false
        }
    }

    private fun closeDescriptor(soundId: Int) {
        try {
            openDescriptors.remove(soundId)?.close()
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock.isHeld) wakeLock.release()
        } catch (_: Exception) {
        }
    }
}
