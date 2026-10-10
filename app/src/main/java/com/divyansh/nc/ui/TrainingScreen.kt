package com.divyansh.nc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divyansh.nc.data.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TrainingScreen(date: LocalDate, allSets: List<WorkoutSet>, dao: NCDao) {
    val day = date.toString()
    val template = WorkoutPlan.forDay(date.dayOfWeek)
    val todays = allSets.filter { it.date == day }
    SectionTitle(template.title, "Set-by-set reps, weight, and previous workout history")
    NCCard {
        Text("Workout progress", style = MaterialTheme.typography.titleMedium)
        val completed = todays.count { item -> template.exercises.any { it.name == item.exercise } }
        val target = template.exercises.sumOf { it.sets }
        LinearProgressIndicator(
            progress = { if (target == 0) 0f else (completed.toFloat() / target).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(), color = NCGreen,
            trackColor = NCSurfaceHigher
        )
        Text("$completed / $target sets saved", color = NCMuted)
    }
    template.exercises.forEach { exercise ->
        ExerciseCard(date, exercise, todays.filter { it.exercise == exercise.name }, allSets, dao)
    }
}

@Composable
private fun ExerciseCard(
    date: LocalDate, exercise: ExerciseTemplate, todays: List<WorkoutSet>,
    history: List<WorkoutSet>, dao: NCDao
) {
    val scope = rememberCoroutineScope()
    val previousDate = history.filter { it.exercise == exercise.name && it.date < date.toString() }
        .maxOfOrNull { it.date }
    val previous = history.filter { it.exercise == exercise.name && it.date == previousDate }
    val maxWeight = history.filter { it.exercise == exercise.name }.maxOfOrNull { it.weightKg }
    val repCap = Regex("\\d+").findAll(exercise.reps).map { it.value.toInt() }.lastOrNull()
    val suggestProgression = previous.isNotEmpty() && repCap != null && previous.all { it.reps >= repCap } &&
        previous.size >= exercise.sets
    var showHistory by remember { mutableStateOf(false) }

    NCCard {
        Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Target: ${exercise.sets} × ${exercise.reps}  •  Start: ${exercise.initialKg?.let { "$it kg" } ?: "choose weight"}",
            color = NCMuted, style = MaterialTheme.typography.bodySmall)
        if (previousDate != null) {
            Text("Last time ($previousDate): " + previous.sortedBy { it.setNumber }
                .joinToString(" | ") { "${it.weightKg}kg×${it.reps}" }, color = NCPurple,
                style = MaterialTheme.typography.bodySmall)
        }
        if (maxWeight != null) Text("Highest logged: ${maxWeight} kg", color = NCGreen, style = MaterialTheme.typography.bodySmall)
        if (suggestProgression) Text("Progression idea: you reached the top reps last session. Consider a small weight increase if form stays solid.",
            color = NCOrange, style = MaterialTheme.typography.bodySmall)
        repeat(exercise.sets) { index ->
            val setNumber = index + 1
            val today = todays.firstOrNull { it.setNumber == setNumber }
            val last = previous.firstOrNull { it.setNumber == setNumber }
            WorkoutSetRow(date, exercise, setNumber, today, last, dao)
        }
        TextButton(onClick = { showHistory = !showHistory }) { Text(if (showHistory) "Hide history" else "View exercise history") }
        if (showHistory) {
            history.filter { it.exercise == exercise.name && it.date < date.toString() }
                .groupBy { it.date }.toList().sortedByDescending { it.first }.take(8).forEach { (onDate, sets) ->
                    Text(onDate + ": " + sets.sortedBy { it.setNumber }.joinToString(" | ") { "${it.weightKg}×${it.reps}" },
                        color = NCMuted, style = MaterialTheme.typography.bodySmall)
                }
        }
    }
}

@Composable
private fun WorkoutSetRow(date: LocalDate, exercise: ExerciseTemplate, setNumber: Int,
                          today: WorkoutSet?, last: WorkoutSet?, dao: NCDao) {
    val scope = rememberCoroutineScope()
    var reps by remember(date, exercise.name, setNumber, today?.timestamp, last?.timestamp) {
        mutableStateOf((today?.reps ?: last?.reps)?.toString() ?: "")
    }
    var weight by remember(date, exercise.name, setNumber, today?.timestamp, last?.timestamp) {
        mutableStateOf((today?.weightKg ?: last?.weightKg ?: exercise.initialKg)?.toString() ?: "")
    }
    val repValue = reps.toIntOrNull()
    val kgValue = weight.toDoubleOrNull()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$setNumber", Modifier.width(22.dp), color = NCMuted)
        OutlinedTextField(reps, { reps = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f), singleLine = true)
        OutlinedTextField(weight, { weight = it }, label = { Text("kg") }, modifier = Modifier.weight(1f), singleLine = true)
        TextButton(
            enabled = repValue != null && repValue >= 0 && kgValue != null && kgValue >= 0,
            onClick = { scope.launch {
                dao.saveSet((today ?: WorkoutSet(date = date.toString(), exercise = exercise.name,
                    setNumber = setNumber, reps = 0, weightKg = 0.0)).copy(
                    timestamp = System.currentTimeMillis(), reps = repValue!!, weightKg = kgValue!!))
            } }
        ) { Text(if (today == null) "Save" else "✓") }
        if (today != null) IconButton(onClick = { scope.launch { dao.deleteSet(today) } }, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Filled.Delete, "Delete set", tint = NCRed)
        }
    }
}
