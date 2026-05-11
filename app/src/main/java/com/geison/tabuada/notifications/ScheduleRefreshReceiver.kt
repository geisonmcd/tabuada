package com.geison.tabuada.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScheduleRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
            NotificationScheduler.schedulePractice(context)
    }

    companion object {
        const val ACTION_REFRESH_SCHEDULE = "com.geison.tabuada.action.REFRESH_SCHEDULE"
    }
}
