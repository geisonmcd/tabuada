package com.geison.tabuada

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.geison.tabuada.data.AnswerResult
import com.geison.tabuada.data.MultiplicationFactProgress
import com.geison.tabuada.data.PracticeRepository
import com.geison.tabuada.notifications.NotificationScheduler
import com.geison.tabuada.notifications.TabuadaNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PracticeRepository(application)
    private val mutableCurrentFact = MutableStateFlow(repository.nextPracticeFact())

    val state = repository.state
    val currentFact = mutableCurrentFact.asStateFlow()

    init {
        repository.recordShown(mutableCurrentFact.value.id)
    }

    fun answerCurrent(rawAnswer: String): AnswerResult? {
        val result = repository.answer(mutableCurrentFact.value.id, rawAnswer) ?: return null
        advanceToNextFact()
        NotificationScheduler.schedulePractice(getApplication())
        return result
    }

    fun skipCurrent() {
        advanceToNextFact()
    }

    fun updateNotificationWindow(startHour: Int, endHour: Int) {
        repository.updateNotificationWindow(
            startHour = startHour.coerceIn(0, 22),
            endHour = endHour.coerceIn(startHour + 1, 23),
        )
        NotificationScheduler.schedulePractice(getApplication())
    }

    fun updatePracticeDay(dayOfWeek: DayOfWeek, isEnabled: Boolean) {
        repository.updatePracticeDay(dayOfWeek, isEnabled)
        NotificationScheduler.schedulePractice(getApplication())
    }

    fun updateNotificationsPerDay(value: Int) {
        repository.updateNotificationsPerDay(value)
        NotificationScheduler.schedulePractice(getApplication())
    }

    fun updateMasteryStreak(value: Int) {
        repository.updateMasteryStreak(value)
        NotificationScheduler.schedulePractice(getApplication())
    }

    fun showMoreNotifications() {
        repository.addExtraNotifications()
        NotificationScheduler.schedulePractice(getApplication())
    }

    fun testNotification(fact: MultiplicationFactProgress = mutableCurrentFact.value) {
        repository.recordShown(fact.id)
        TabuadaNotifier.showQuestion(
            context = getApplication(),
            notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            fact = fact,
        )
    }

    fun rescheduleNow() {
        NotificationScheduler.schedulePractice(getApplication())
    }

    private fun advanceToNextFact() {
        val nextFact = repository.nextPracticeFact()
        mutableCurrentFact.value = nextFact
        repository.recordShown(nextFact.id)
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(application) as T
                }
            }
        }
    }
}
