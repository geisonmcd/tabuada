package com.geison.tabuada.notifications

import com.geison.tabuada.data.AppState
import com.geison.tabuada.data.MultiplicationFactProgress
import com.geison.tabuada.data.NotificationWindowSettings
import com.geison.tabuada.data.PracticeSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

class NotificationSchedulerTest {
    @Test
    fun createSchedulePlanQueuesTwoQuestionsByDefault() {
        val startDay = LocalDate.of(2026, 5, 11)
        val facts = listOf(
            MultiplicationFactProgress(7, 8),
            MultiplicationFactProgress(8, 7),
        )

        val plan = NotificationScheduler.createSchedulePlan(
            state = AppState(
                facts = facts,
                notificationWindow = NotificationWindowSettings(9, 10),
            ),
            startDay = startDay,
            totalDays = 1,
            random = Random(1),
            dueFactsForDay = { facts },
        )

        assertEquals(2, plan.size)
        assertEquals(setOf("7-8", "8-7"), plan.map { it.fact.id }.toSet())
    }

    @Test
    fun createSchedulePlanHonorsPracticeDays() {
        val startDay = LocalDate.of(2026, 5, 11)
        val facts = listOf(MultiplicationFactProgress(7, 8))

        val plan = NotificationScheduler.createSchedulePlan(
            state = AppState(
                facts = facts,
                practiceDays = setOf(DayOfWeek.WEDNESDAY),
                settings = PracticeSettings(notificationsPerDay = 2),
            ),
            startDay = startDay,
            totalDays = 2,
            dueFactsForDay = { facts },
        )

        assertEquals(0, plan.size)
    }
}
