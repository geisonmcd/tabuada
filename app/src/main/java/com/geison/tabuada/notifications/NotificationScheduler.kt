package com.geison.tabuada.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.geison.tabuada.data.AppState
import com.geison.tabuada.data.MultiplicationFactProgress
import com.geison.tabuada.data.PracticeRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

object NotificationScheduler {
    private const val QUESTION_REQUEST_CODE_BASE = 2_000
    private const val REFRESH_REQUEST_CODE = 9_000
    private const val SCHEDULE_HORIZON_DAYS = 7
    private const val MAX_SCHEDULED_ALARMS = 96
    private val notificationWindowMillis = Duration.ofMinutes(10).toMillis()

    fun schedulePractice(context: Context) {
        NotificationChannels.ensureCreated(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        cancelQuestionAlarms(context, alarmManager)
        scheduleTomorrowRefresh(context, alarmManager)

        val repository = PracticeRepository(context)
        val plan = createSchedulePlan(
            state = repository.state.value,
            startDay = LocalDate.now(),
            dueFactsForDay = { day -> repository.notificationFactsForDay(day) },
        )

        val now = LocalDateTime.now()
        plan.filter { it.triggerAt.isAfter(now.plusMinutes(1)) }
            .forEachIndexed { index, item ->
                val intent = Intent(context, NotificationReceiver::class.java)
                    .setAction(NotificationReceiver.ACTION_SHOW_QUESTION)
                    .putExtra(NotificationReceiver.EXTRA_FACT_ID, item.fact.id)
                    .putExtra(NotificationReceiver.EXTRA_LEFT, item.fact.left)
                    .putExtra(NotificationReceiver.EXTRA_RIGHT, item.fact.right)
                    .putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, item.notificationId)

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    QUESTION_REQUEST_CODE_BASE + index,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    item.triggerAt.toEpochMillis(),
                    notificationWindowMillis,
                    pendingIntent,
                )
            }
    }

    internal fun createSchedulePlan(
        state: AppState,
        startDay: LocalDate,
        totalDays: Int = SCHEDULE_HORIZON_DAYS,
        random: Random = Random.Default,
        dueFactsForDay: (LocalDate) -> List<MultiplicationFactProgress>,
    ): List<ScheduledQuestion> {
        return (0 until totalDays.coerceAtLeast(0))
            .flatMap { offset ->
                val day = startDay.plusDays(offset.toLong())
                createDayPlan(state, day, dueFactsForDay(day), random)
            }
            .sortedBy { it.triggerAt }
            .take(MAX_SCHEDULED_ALARMS)
            .mapIndexed { index, question -> question.copy(notificationId = 5_000 + index) }
    }

    private fun createDayPlan(
        state: AppState,
        day: LocalDate,
        facts: List<MultiplicationFactProgress>,
        random: Random,
    ): List<ScheduledQuestion> {
        if (day.dayOfWeek !in state.practiceDays || facts.isEmpty()) return emptyList()
        val startMinute = state.notificationWindow.startHour * 60
        val endMinuteExclusive = state.notificationWindow.endHour * 60
        if (startMinute >= endMinuteExclusive) return emptyList()

        val usedMinutes = mutableSetOf<Int>()
        return facts.mapNotNull { fact ->
            val candidates = (startMinute until endMinuteExclusive).filterNot(usedMinutes::contains)
            if (candidates.isEmpty()) return@mapNotNull null
            val minute = candidates.random(random)
            usedMinutes += minute
            ScheduledQuestion(
                notificationId = 0,
                fact = fact,
                triggerAt = day.atStartOfDay().plusMinutes(minute.toLong()),
            )
        }
    }

    private fun cancelQuestionAlarms(context: Context, alarmManager: AlarmManager) {
        repeat(MAX_SCHEDULED_ALARMS) { index ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                QUESTION_REQUEST_CODE_BASE + index,
                Intent(context, NotificationReceiver::class.java)
                    .setAction(NotificationReceiver.ACTION_SHOW_QUESTION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleTomorrowRefresh(context: Context, alarmManager: AlarmManager) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REFRESH_REQUEST_CODE,
            Intent(context, ScheduleRefreshReceiver::class.java)
                .setAction(ScheduleRefreshReceiver.ACTION_REFRESH_SCHEDULE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            LocalDate.now().plusDays(1).atStartOfDay().plusMinutes(5).toEpochMillis(),
            notificationWindowMillis,
            pendingIntent,
        )
    }

    private fun LocalDateTime.toEpochMillis(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

data class ScheduledQuestion(
    val notificationId: Int,
    val fact: MultiplicationFactProgress,
    val triggerAt: LocalDateTime,
)
