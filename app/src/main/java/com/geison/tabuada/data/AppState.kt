package com.geison.tabuada.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate

const val TABLE_MIN = 1
const val TABLE_MAX = 10
const val DEFAULT_DAILY_NOTIFICATIONS = 2
const val MAX_DAILY_NOTIFICATIONS = 12
val DEFAULT_PRACTICE_DAYS: Set<DayOfWeek> = DayOfWeek.values().toSet()

@Serializable
data class NotificationWindowSettings(
    val startHour: Int = 9,
    val endHour: Int = 20,
)

@Serializable
data class PracticeSettings(
    val notificationsPerDay: Int = DEFAULT_DAILY_NOTIFICATIONS,
    val extraNotificationsPerDay: Int = 0,
    val masteryStreak: Int = 100,
)

@Serializable
data class AttemptRecord(
    val epochDay: Long,
    val answeredAtEpochMillis: Long,
    val answer: Int,
    val correct: Boolean,
)

@Serializable
data class MultiplicationFactProgress(
    val left: Int,
    val right: Int,
    val timesShown: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val currentCorrectStreak: Int = 0,
    val fibonacciIndex: Int = 0,
    val intensiveReview: Boolean = false,
    val dueEpochDay: Long = LocalDate.now().toEpochDay(),
    val attempts: List<AttemptRecord> = emptyList(),
) {
    val id: String get() = "$left-$right"
    val label: String get() = "${left}x$right"
    val answer: Int get() = left * right
    fun isLearned(settings: PracticeSettings): Boolean = currentCorrectStreak >= settings.masteryStreak
}

@Serializable
data class AppState(
    val facts: List<MultiplicationFactProgress> = defaultFacts(),
    val notificationWindow: NotificationWindowSettings = NotificationWindowSettings(),
    val practiceDays: Set<DayOfWeek> = DEFAULT_PRACTICE_DAYS,
    val settings: PracticeSettings = PracticeSettings(),
)

fun defaultFacts(): List<MultiplicationFactProgress> {
    return (TABLE_MIN..TABLE_MAX).flatMap { left ->
        (TABLE_MIN..TABLE_MAX).map { right ->
            MultiplicationFactProgress(left = left, right = right)
        }
    }
}
