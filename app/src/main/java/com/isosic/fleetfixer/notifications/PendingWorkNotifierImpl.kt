package com.isosic.fleetfixer.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.isosic.fleetfixer.MainActivity
import com.isosic.fleetfixer.R
import com.isosic.fleetfixer.core.domain.PendingWorkNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PendingWorkNotifierImpl(
    private val context: Context,
    private val applicationScope: CoroutineScope
) : PendingWorkNotifier {

    private val mutex = Mutex()
    private val pendingCounts = mutableMapOf<String, Int>()
    private val debounceJobs = mutableMapOf<String, Job>()

    init {
        ensureChannel()
    }

    override fun notifyPendingWorkAdded(bikeId: String) {
        applicationScope.launch {
            mutex.withLock {
                pendingCounts[bikeId] = (pendingCounts[bikeId] ?: 0) + 1
                debounceJobs[bikeId]?.cancel()
                debounceJobs[bikeId] = applicationScope.launch {
                    delay(DEBOUNCE_MS)
                    val count = mutex.withLock {
                        val value = pendingCounts.remove(bikeId) ?: 0
                        debounceJobs.remove(bikeId)
                        value
                    }
                    if (count > 0) {
                        postNotification(bikeId)
                    }
                }
            }
        }
    }

    private fun postNotification(bikeId: String) {
        if (!canPostNotifications()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            bikeId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_BIKE_ID, bikeId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.pending_work_notification_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_TAG, bikeId.hashCode(), notification)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.pending_work_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.pending_work_notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_OPEN_BIKE_ID = "extra_open_bike_id"
        private const val CHANNEL_ID = "pending_work"
        private const val NOTIFICATION_TAG = "pending_work"
        private const val DEBOUNCE_MS = 1_500L
    }
}
