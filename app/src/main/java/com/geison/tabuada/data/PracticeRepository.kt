package com.geison.tabuada.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.max

class PracticeRepository(private val context: Context) {
    private val mutableState = MutableStateFlow(PracticeStorage.load(context).normalized())
    val state: StateFlow<AppState> = mutableState.asStateFlow()

    fun recordShown(factId: String) {
        updateFact(factId) { it.copy(timesShown = it.timesShown + 1) }
    }

    fun answer(
        factId: String,
        rawAnswer: String,
        today: LocalDate = LocalDate.now(),
    ): AnswerResult? {
        val answer = rawAnswer.trim().toIntOrNull() ?: return null
        var result: AnswerResult? = null
        updateFact(factId) { fact ->
            val correct = answer == fact.answer
            val todayAttempts = fact.attempts.count { it.epochDay == today.toEpochDay() }
            val nextFact = if (correct) {
                val nextIndex = (fact.fibonacciIndex + 1).coerceAtMost(FIBONACCI_DELAYS.lastIndex)
                fact.copy(
                    correctCount = fact.correctCount + 1,
                    currentCorrectStreak = fact.currentCorrectStreak + 1,
                    fibonacciIndex = nextIndex,
                    intensiveReview = false,
                    dueEpochDay = today.plusDays(FIBONACCI_DELAYS[nextIndex].toLong()).toEpochDay(),
                )
            } else {
                val wrongsToday = fact.attempts.count { it.epochDay == today.toEpochDay() && !it.correct } + 1
                fact.copy(
                    wrongCount = fact.wrongCount + 1,
                    currentCorrectStreak = 0,
                    fibonacciIndex = 0,
                    intensiveReview = fact.intensiveReview || wrongsToday >= 2,
                    dueEpochDay = if (wrongsToday >= 2) today.plusDays(1).toEpochDay() else today.toEpochDay(),
                )
            }.copy(
                attempts = (fact.attempts + AttemptRecord(
                    epochDay = today.toEpochDay(),
                    answeredAtEpochMillis = System.currentTimeMillis(),
                    answer = answer,
                    correct = correct,
                )).takeLast(MAX_ATTEMPTS_PER_FACT),
            )
            result = AnswerResult(nextFact, answer, correct, todayAttempts + 1)
            nextFact
        }
        return result
    }

    fun nextPracticeFact(today: LocalDate = LocalDate.now()): MultiplicationFactProgress {
        return rankedFacts(today).first()
    }

    fun notificationFactsForDay(day: LocalDate = LocalDate.now()): List<MultiplicationFactProgress> {
        val state = mutableState.value
        if (day.dayOfWeek !in state.practiceDays) return emptyList()
        val target = (state.settings.notificationsPerDay + state.settings.extraNotificationsPerDay)
            .coerceIn(1, MAX_DAILY_NOTIFICATIONS)
        return rankedFacts(day)
            .filterNot { it.isLearned(state.settings) }
            .filter { it.notificationsRemainingToday(day) > 0 }
            .take(target)
    }

    fun updateNotificationWindow(startHour: Int, endHour: Int) {
        updateState {
            copy(notificationWindow = NotificationWindowSettings(startHour, endHour))
        }
    }

    fun updatePracticeDay(dayOfWeek: DayOfWeek, enabled: Boolean) {
        updateState {
            copy(practiceDays = if (enabled) practiceDays + dayOfWeek else practiceDays - dayOfWeek)
        }
    }

    fun updateNotificationsPerDay(value: Int) {
        updateState {
            copy(settings = settings.copy(notificationsPerDay = value.coerceIn(1, MAX_DAILY_NOTIFICATIONS)))
        }
    }

    fun updateMasteryStreak(value: Int) {
        updateState {
            copy(settings = settings.copy(masteryStreak = value.coerceIn(3, 500)))
        }
    }

    fun addExtraNotifications() {
        updateState {
            copy(
                settings = settings.copy(
                    extraNotificationsPerDay = (settings.extraNotificationsPerDay + 2).coerceAtMost(MAX_DAILY_NOTIFICATIONS),
                ),
            )
        }
    }

    fun resetExtraNotifications() {
        updateState {
            copy(settings = settings.copy(extraNotificationsPerDay = 0))
        }
    }

    private fun rankedFacts(today: LocalDate): List<MultiplicationFactProgress> {
        val epochDay = today.toEpochDay()
        val state = mutableState.value
        val active = state.facts.filterNot { it.isLearned(state.settings) }
        val pool = active.ifEmpty { state.facts }
        return pool.sortedWith(
            compareByDescending<MultiplicationFactProgress> { it.priority(epochDay) }
                .thenBy { it.correctCount - it.wrongCount }
                .thenBy { it.label },
        )
    }

    private fun updateFact(
        factId: String,
        transform: (MultiplicationFactProgress) -> MultiplicationFactProgress,
    ) {
        updateState {
            copy(facts = facts.map { if (it.id == factId) transform(it) else it })
        }
    }

    private fun updateState(transform: AppState.() -> AppState) {
        val updated = mutableState.value.transform().normalized()
        mutableState.value = updated
        PracticeStorage.save(context, updated)
    }

    private fun AppState.normalized(): AppState {
        val byId = facts.associateBy { it.id }
        return copy(facts = defaultFacts().map { byId[it.id] ?: it })
    }

    companion object {
        private const val MAX_ATTEMPTS_PER_FACT = 120
        private val FIBONACCI_DELAYS = listOf(0, 2, 3, 5, 8, 13, 21, 34, 55)
    }
}

data class AnswerResult(
    val fact: MultiplicationFactProgress,
    val answer: Int,
    val correct: Boolean,
    val attemptNumberToday: Int,
)

fun MultiplicationFactProgress.notificationsRemainingToday(day: LocalDate): Int {
    val epochDay = day.toEpochDay()
    val attemptsToday = attempts.filter { it.epochDay == epochDay }
    if (attemptsToday.any { it.correct }) return 0
    val wrongsToday = attemptsToday.count { !it.correct }
    val target = when {
        intensiveReview -> 2
        wrongsToday >= 2 -> 2
        wrongsToday == 1 -> 1
        dueEpochDay <= epochDay -> 1
        else -> 0
    }
    return max(0, target - attemptsToday.size)
}

private fun MultiplicationFactProgress.priority(epochDay: Long): Int {
    val recencyPenalty = attempts.count { it.epochDay >= epochDay - 6 && it.correct } * 4
    val dueBoost = if (dueEpochDay <= epochDay) 30 else 0
    return dueBoost + wrongCount * 8 - correctCount * 2 - recencyPenalty
}
