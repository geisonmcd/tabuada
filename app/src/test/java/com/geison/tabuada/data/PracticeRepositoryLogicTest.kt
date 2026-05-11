package com.geison.tabuada.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PracticeRepositoryLogicTest {
    @Test
    fun correctAnswersMoveDueDateThroughFibonacciDelays() {
        val today = LocalDate.of(2026, 5, 10)
        val fact = MultiplicationFactProgress(
            left = 7,
            right = 8,
            dueEpochDay = today.toEpochDay(),
        )

        val first = answered(fact, correct = true, today = today)
        val second = answered(first, correct = true, today = today.plusDays(2))
        val third = answered(second, correct = true, today = today.plusDays(5))

        assertEquals(today.plusDays(2).toEpochDay(), first.dueEpochDay)
        assertEquals(today.plusDays(5).toEpochDay(), second.dueEpochDay)
        assertEquals(today.plusDays(10).toEpochDay(), third.dueEpochDay)
    }

    @Test
    fun wrongAnswersKeepFactAvailableThenDailyTwice() {
        val today = LocalDate.of(2026, 5, 10)
        val fact = MultiplicationFactProgress(
            left = 7,
            right = 8,
            dueEpochDay = today.toEpochDay(),
        )
        val onceWrong = fact.withAttempt(today, correct = false)
        val twiceWrong = onceWrong.withAttempt(today, correct = false)

        assertEquals(1, fact.notificationsRemainingToday(today))
        assertEquals(0, onceWrong.notificationsRemainingToday(today))
        assertEquals(0, twiceWrong.notificationsRemainingToday(today))
        assertEquals(2, twiceWrong.notificationsRemainingToday(today.plusDays(1)))
    }

    @Test
    fun masteryThresholdMarksFactLearned() {
        val settings = PracticeSettings(masteryStreak = 3)
        val fact = MultiplicationFactProgress(left = 1, right = 1, currentCorrectStreak = 3)

        assertTrue(fact.isLearned(settings))
        assertFalse(fact.copy(currentCorrectStreak = 2).isLearned(settings))
    }

    private fun answered(
        fact: MultiplicationFactProgress,
        correct: Boolean,
        today: LocalDate,
    ): MultiplicationFactProgress {
        val nextIndex = if (correct) (fact.fibonacciIndex + 1).coerceAtMost(8) else 0
        val delays = listOf(0, 2, 3, 5, 8, 13, 21, 34, 55)
        return fact.copy(
            correctCount = fact.correctCount + if (correct) 1 else 0,
            wrongCount = fact.wrongCount + if (correct) 0 else 1,
            currentCorrectStreak = if (correct) fact.currentCorrectStreak + 1 else 0,
            fibonacciIndex = nextIndex,
            intensiveReview = false,
            dueEpochDay = today.plusDays(delays[nextIndex].toLong()).toEpochDay(),
            attempts = fact.attempts + AttemptRecord(today.toEpochDay(), 0, fact.answer, correct),
        )
    }

    private fun MultiplicationFactProgress.withAttempt(
        today: LocalDate,
        correct: Boolean,
    ): MultiplicationFactProgress {
        val wrongsToday = attempts.count { it.epochDay == today.toEpochDay() && !it.correct } + if (correct) 0 else 1
        return copy(
            wrongCount = wrongCount + if (correct) 0 else 1,
            intensiveReview = wrongsToday >= 2,
            dueEpochDay = if (wrongsToday >= 2) today.plusDays(1).toEpochDay() else today.toEpochDay(),
            attempts = attempts + AttemptRecord(today.toEpochDay(), 0, answer + 1, correct),
        )
    }
}
