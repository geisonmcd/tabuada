package com.geison.tabuada.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.geison.tabuada.R
import com.geison.tabuada.data.MultiplicationFactProgress
import com.geison.tabuada.data.PracticeRepository

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER_QUESTION -> answerQuestion(context, intent)
            ACTION_MORE_QUESTIONS -> {
                PracticeRepository(context).addExtraNotifications()
                NotificationScheduler.schedulePractice(context)
                NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
            }
            ACTION_SHOW_QUESTION -> showQuestion(context, intent)
        }
    }

    private fun showQuestion(context: Context, intent: Intent) {
        if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val fact = factFromIntent(intent) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        PracticeRepository(context).recordShown(fact.id)
        TabuadaNotifier.showQuestion(context, notificationId, fact)
    }

    private fun answerQuestion(context: Context, intent: Intent) {
        val fact = factFromIntent(intent) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val answer = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(EXTRA_ANSWER)
            ?.toString()
            .orEmpty()

        val result = PracticeRepository(context).answer(fact.id, answer)
        if (result == null) {
            TabuadaNotifier.showResult(
                context = context,
                notificationId = notificationId,
                title = context.getString(R.string.notification_result_invalid_title),
                text = context.getString(R.string.notification_result_invalid_text),
            )
            return
        }

        val title = if (result.correct) {
            context.getString(R.string.notification_result_correct_title)
        } else {
            context.getString(R.string.notification_result_wrong_title)
        }
        val text = if (result.correct) {
            context.getString(R.string.notification_result_correct_text, result.fact.left, result.fact.right)
        } else {
            context.getString(
                R.string.notification_result_wrong_text,
                result.fact.left,
                result.fact.right,
                result.fact.answer,
            )
        }
        TabuadaNotifier.showResult(context, notificationId, title, text)
        NotificationScheduler.schedulePractice(context)
    }

    private fun factFromIntent(intent: Intent): MultiplicationFactProgress? {
        val factId = intent.getStringExtra(EXTRA_FACT_ID) ?: return null
        val left = intent.getIntExtra(EXTRA_LEFT, 0)
        val right = intent.getIntExtra(EXTRA_RIGHT, 0)
        if (left <= 0 || right <= 0) return null
        return MultiplicationFactProgress(left = left, right = right).also {
            check(it.id == factId)
        }
    }

    companion object {
        const val ACTION_SHOW_QUESTION = "com.geison.tabuada.action.SHOW_QUESTION"
        const val ACTION_ANSWER_QUESTION = "com.geison.tabuada.action.ANSWER_QUESTION"
        const val ACTION_MORE_QUESTIONS = "com.geison.tabuada.action.MORE_QUESTIONS"
        const val EXTRA_FACT_ID = "extra_fact_id"
        const val EXTRA_LEFT = "extra_left"
        const val EXTRA_RIGHT = "extra_right"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_ANSWER = "extra_answer"
    }
}
