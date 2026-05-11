package com.geison.tabuada.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.geison.tabuada.R

object NotificationChannels {
    const val PRACTICE_CHANNEL_ID = "practice"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            PRACTICE_CHANNEL_ID,
            context.getString(R.string.notification_channel_practice),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_practice_description)
        }
        manager.createNotificationChannel(channel)
    }
}
