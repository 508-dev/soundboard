package dev.co508.soundboard.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.co508.soundboard.MainActivity
import dev.co508.soundboard.R
import dev.co508.soundboard.SoundboardApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the process alive while sounds are looping, and shows the one
 * notification Android requires in exchange.
 *
 * This service does **not** own the players — [SoundboardEngine] does, from the
 * Application. The service only mirrors engine state into a notification and
 * stops itself once nothing is audible, which keeps the tap-to-play path free
 * of any service binding. See `DECISIONS.md` → "Engine In Application, Service
 * For Lifetime Only".
 */
class PlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine by lazy { (application as SoundboardApp).engine }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        scope.launch {
            engine.statuses
                .map { statuses -> statuses.count { it.value == PlaybackStatus.PLAYING } }
                .collect { playingCount ->
                    if (playingCount == 0) {
                        stopSelf()
                    } else {
                        notificationManager.notify(NOTIFICATION_ID, buildNotification(playingCount))
                    }
                }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP_ALL) {
            engine.pauseAll()
        }

        // Android gives us ~5s from startForegroundService() to get here, so
        // promote first and let the collector above refine the text after.
        val playingCount = engine.statuses.value.count { it.value == PlaybackStatus.PLAYING }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(playingCount),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )

        if (playingCount == 0) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_channel_name),
                // LOW: this is an ambient status readout, not something to buzz about.
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.playback_channel_description)
                setShowBadge(false)
            }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(playingCount: Int): Notification {
        val openApp =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val stopAll =
            PendingIntent.getService(
                this,
                1,
                Intent(this, PlaybackService::class.java).setAction(ACTION_STOP_ALL),
                PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_playing)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(resources.getQuantityString(R.plurals.sounds_playing, playingCount, playingCount))
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.stop_all), stopAll)
            .setOngoing(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP_ALL = "dev.co508.soundboard.STOP_ALL"

        /**
         * Ensures the service is running while something is playing.
         *
         * Safe to call on every play tap; the service stops itself when the
         * engine goes silent.
         */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, PlaybackService::class.java))
        }
    }
}
