package com.geison.tabuada.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geison.tabuada.MainViewModel
import com.geison.tabuada.data.AppState
import com.geison.tabuada.data.MultiplicationFactProgress
import java.time.DayOfWeek
import java.time.LocalDate

private val Ink = Color(0xFF15202B)
private val Background = Color(0xFFF6F8F3)
private val Panel = Color(0xFFFFFFFF)
private val LearnedGreen = Color(0xFFE1F4E7)
private val WeakRed = Color(0xFFFFE7E1)
private val Accent = Color(0xFF2F6F4E)

private val TabuadaColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Background,
    onBackground = Ink,
    surface = Panel,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EEE7),
)

@Composable
fun TabuadaApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentFact by viewModel.currentFact.collectAsStateWithLifecycle()
    var answer by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showLearned by rememberSaveable { mutableStateOf(true) }

    MaterialTheme(colorScheme = TabuadaColors) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Accent)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Tabuada do 10",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Configurações", tint = Color.White)
                    }
                }
            },
            containerColor = Background,
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    PracticeCard(
                        fact = currentFact,
                        answer = answer,
                        feedback = feedback,
                        onAnswerChange = {
                            answer = it.filter(Char::isDigit).take(4)
                        },
                        onSubmit = {
                            val result = viewModel.answerCurrent(answer)
                            feedback = when {
                                result == null -> "Digite um número."
                                result.correct -> "Certo. ${result.fact.label} vai descansar pela sequência espaçada."
                                else -> "Errado. A resposta era ${result.fact.answer}."
                            }
                            answer = ""
                        },
                        onSkip = {
                            viewModel.skipCurrent()
                            feedback = null
                            answer = ""
                        },
                        onTestNotification = { viewModel.testNotification(currentFact) },
                    )
                }

                if (showSettings) {
                    item {
                        SettingsCard(state = state, viewModel = viewModel)
                    }
                }

                item {
                    StatsHeader(
                        state = state,
                        showLearned = showLearned,
                        onShowLearnedChange = { showLearned = it },
                    )
                }

                items(
                    state.facts
                        .filter { showLearned || !it.isLearned(state.settings) }
                        .sortedWith(
                            compareBy<MultiplicationFactProgress> { it.isLearned(state.settings) }
                                .thenByDescending { it.wrongCount }
                                .thenBy { it.correctCount }
                                .thenBy { it.left }
                                .thenBy { it.right },
                        ),
                    key = { it.id },
                ) { fact ->
                    FactRow(fact = fact, state = state)
                }
            }
        }
    }
}

@Composable
private fun PracticeCard(
    fact: MultiplicationFactProgress,
    answer: String,
    feedback: String?,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    onTestNotification: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Quanto é", style = MaterialTheme.typography.titleMedium)
            Text(
                "${fact.left} x ${fact.right} ?",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                label = { Text("Resposta") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSubmit, enabled = answer.isNotBlank()) {
                    Text("Responder")
                }
                TextButton(onClick = onSkip) {
                    Text("Pular")
                }
                IconButton(onClick = onTestNotification) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Testar notificação")
                }
            }
            feedback?.let {
                Text(it, color = Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SettingsCard(state: AppState, viewModel: MainViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Configurações", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            StepperRow(
                label = "Notificações por dia",
                value = state.settings.notificationsPerDay,
                onMinus = { viewModel.updateNotificationsPerDay(state.settings.notificationsPerDay - 1) },
                onPlus = { viewModel.updateNotificationsPerDay(state.settings.notificationsPerDay + 1) },
            )
            StepperRow(
                label = "Acertos seguidos para aprender",
                value = state.settings.masteryStreak,
                onMinus = { viewModel.updateMasteryStreak(state.settings.masteryStreak - 1) },
                onPlus = { viewModel.updateMasteryStreak(state.settings.masteryStreak + 1) },
            )
            StepperRow(
                label = "Início",
                value = state.notificationWindow.startHour,
                onMinus = {
                    viewModel.updateNotificationWindow(
                        state.notificationWindow.startHour - 1,
                        state.notificationWindow.endHour,
                    )
                },
                onPlus = {
                    viewModel.updateNotificationWindow(
                        state.notificationWindow.startHour + 1,
                        state.notificationWindow.endHour,
                    )
                },
            )
            StepperRow(
                label = "Fim",
                value = state.notificationWindow.endHour,
                onMinus = {
                    viewModel.updateNotificationWindow(
                        state.notificationWindow.startHour,
                        state.notificationWindow.endHour - 1,
                    )
                },
                onPlus = {
                    viewModel.updateNotificationWindow(
                        state.notificationWindow.startHour,
                        state.notificationWindow.endHour + 1,
                    )
                },
            )
            DayOfWeek.values().forEach { day ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = day in state.practiceDays,
                        onCheckedChange = { viewModel.updatePracticeDay(day, it) },
                    )
                    Text(day.name.lowercase().replaceFirstChar(Char::uppercase))
                }
            }
        }
    }
}

@Composable
private fun StepperRow(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        TextButton(onClick = onMinus) { Text("-") }
        Text(value.toString(), fontWeight = FontWeight.Bold)
        TextButton(onClick = onPlus) { Text("+") }
    }
}

@Composable
private fun StatsHeader(state: AppState, showLearned: Boolean, onShowLearnedChange: (Boolean) -> Unit) {
    val learned = state.facts.count { it.isLearned(state.settings) }
    val wrongs = state.facts.sumOf { it.wrongCount }
    val correct = state.facts.sumOf { it.correctCount }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("Aprendidas", learned.toString())
            StatPill("Acertos", correct.toString())
            StatPill("Erros", wrongs.toString())
        }
        FilterChip(
            selected = showLearned,
            onClick = { onShowLearnedChange(!showLearned) },
            label = { Text("Mostrar aprendidas") },
            leadingIcon = { Icon(Icons.Outlined.TaskAlt, contentDescription = null) },
        )
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Surface(color = Color(0xFFE8EEE7), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FactRow(fact: MultiplicationFactProgress, state: AppState) {
    val learned = fact.isLearned(state.settings)
    val lastWeek = fact.attempts.count { it.epochDay >= LocalDate.now().minusDays(6).toEpochDay() }
    val color = when {
        learned -> LearnedGreen
        fact.wrongCount > fact.correctCount -> WeakRed
        else -> Panel
    }
    Card(colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(fact.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Mostrada ${fact.timesShown}x · semana $lastWeek · sequência ${fact.currentCorrectStreak}/${state.settings.masteryStreak}")
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("✓ ${fact.correctCount}", color = Accent, fontWeight = FontWeight.Bold)
                Text("× ${fact.wrongCount}", color = Color(0xFFB23A2F), fontWeight = FontWeight.Bold)
            }
        }
    }
}
