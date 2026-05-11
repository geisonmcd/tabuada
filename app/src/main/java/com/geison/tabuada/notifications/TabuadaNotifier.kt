package com.geison.tabuada.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.geison.tabuada.MainActivity
import com.geison.tabuada.R
import com.geison.tabuada.data.MultiplicationFactProgress

object TabuadaNotifier {
    private const val ANSWER_ACTION_REQUEST_CODE_BASE = 20_000
    private const val MORE_ACTION_REQUEST_CODE_BASE = 30_000

    fun showQuestion(
        context: Context,
        notificationId: Int,
        fact: MultiplicationFactProgress,
    ) {
        NotificationChannels.ensureCreated(context)
        val openAppIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val answerIntent = PendingIntent.getBroadcast(
            context,
            ANSWER_ACTION_REQUEST_CODE_BASE + notificationId,
            Intent(context, NotificationReceiver::class.java)
                .setAction(NotificationReceiver.ACTION_ANSWER_QUESTION)
                .putExtra(NotificationReceiver.EXTRA_FACT_ID, fact.id)
                .putExtra(NotificationReceiver.EXTRA_LEFT, fact.left)
                .putExtra(NotificationReceiver.EXTRA_RIGHT, fact.right)
                .putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val remoteInput = RemoteInput.Builder(NotificationReceiver.EXTRA_ANSWER)
            .setLabel(context.getString(R.string.notification_answer_hint))
            .build()
        val answerAction = NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.notification_action_answer),
            answerIntent,
        ).addRemoteInput(remoteInput).build()

        val moreIntent = PendingIntent.getBroadcast(
            context,
            MORE_ACTION_REQUEST_CODE_BASE + notificationId,
            Intent(context, NotificationReceiver::class.java)
                .setAction(NotificationReceiver.ACTION_MORE_QUESTIONS)
                .putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        NotificationManagerCompat.from(context).notify(
            notificationId,
            NotificationCompat.Builder(context, NotificationChannels.PRACTICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_title_question))
                .setContentText(context.getString(R.string.notification_question_text, fact.left, fact.right))
                .setContentIntent(openAppIntent)
                .setAutoCancel(false)
                .addAction(answerAction)
                .addAction(0, context.getString(R.string.notification_action_more), moreIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build(),
        )
    }

    fun showResult(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
    ) {
        NotificationManagerCompat.from(context).notify(
            notificationId,
            NotificationCompat.Builder(context, NotificationChannels.PRACTICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build(),
        )
    }
}
