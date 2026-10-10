package com.divyansh.nc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divyansh.nc.data.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(
    date: LocalDate,
    settings: NCSettings,
    allEntries: List<NutritionEntry>,
    budgets: List<DailyBudget>,
    activities: List<ActivityLog>,
    weights: List<WeightLog>,
    dao: NCDao
) {
    val scope = rememberCoroutineScope()
    val day = date.toString()
    val items = allEntries.filter { it.date == day }.sortedBy { it.timestamp }
    val calories = items.sumOf { it.calories }
    val protein = items.sumOf { it.protein }
    val budget = BudgetMath.effective(date, settings, allEntries, budgets)
    val proteinGoal = BudgetMath.proteinGoal(day, settings, budgets)
    val savedBudget = budgets.firstOrNull { it.date == day }
    val activity = activities.firstOrNull { it.date == day }
    val weight = weights.filter { it.date <= day }.maxByOrNull { it.date }

    var showNutritionEditor by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<NutritionEntry?>(null) }
    var deleting by remember { mutableStateOf<NutritionEntry?>(null) }
    var showBudgetEditor by remember { mutableStateOf(false) }
    var showActivityEditor by remember { mutableStateOf(false) }
    var showWeightEditor by remember { mutableStateOf(false) }

    SectionTitle("Daily Nutrition", "Independent of your meal-reference tables")
    NCCard { ConcentricMacroRings(calories, budget, protein, proteinGoal) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricPill("Calories left", "${budget - calories} kcal", if (budget >= calories) NCGreen else NCRed, Modifier.weight(1f))
        MetricPill("Protein left", "${(proteinGoal - protein).roundToInt()}g", NCYellow, Modifier.weight(1f))
    }
    if (budget == 0) Text(
        "Budget is 0 after rollover. This is accounting, NOT advice to fast; use Edit budget to choose an appropriate intake.",
        color = NCOrange, style = MaterialTheme.typography.bodySmall
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { showBudgetEditor = true }, modifier = Modifier.weight(1f)) { Text("Edit daily targets") }
        Button(onClick = { editItem = null; showNutritionEditor = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Add, null)
            Text("Add food")
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = savedBudget?.completed ?: false,
            onCheckedChange = { completed -> scope.launch {
                dao.saveBudget((savedBudget ?: DailyBudget(day)).copy(completed = completed))
            } }
        )
        Text("Mark this day fully logged", color = NCMuted, style = MaterialTheme.typography.bodySmall)
    }

    NCCard {
        Text("Food entries", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        if (items.isEmpty()) Text("No food logged for this date.", color = NCMuted)
        items.forEach { item ->
            HorizontalDivider(color = NCSurfaceHigher)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.food, fontWeight = FontWeight.SemiBold)
                    Text("${item.calories} kcal · ${item.protein}g protein · ${displayTime(item.timestamp)}", color = NCMuted,
                        style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { editItem = item; showNutritionEditor = true }) { Icon(Icons.Filled.Edit, "Edit") }
                IconButton(onClick = { deleting = item }) { Icon(Icons.Filled.Delete, "Delete", tint = NCRed) }
            }
        }
    }

    SectionTitle("Daily activity", "Manual steps and push-ups for v1")
    NCCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricPill("Steps", "${activity?.steps ?: 0} / ${settings.stepGoal}", NCGreen, Modifier.weight(1f))
            MetricPill("Push-ups", "${activity?.pushups ?: 0} / ${settings.pushupGoal}", NCOrange, Modifier.weight(1f))
        }
        OutlinedButton(onClick = { showActivityEditor = true }, modifier = Modifier.fillMaxWidth()) { Text("Update activity counts") }
    }

    SectionTitle("Body metrics", "Weight and BMI, calculated from your profile")
    NCCard {
        val bmi = weight?.let { BudgetMath.bmi(it.weightKg, settings.heightCm) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricPill("Weight", weight?.let { "${it.weightKg} kg" } ?: "Not set", NCPurple, Modifier.weight(1f))
            MetricPill("BMI", bmi?.let { "%.1f".format(it) } ?: "—", NCYellow, Modifier.weight(1f))
        }
        Text("BMI is a screening metric, not a body-fat measurement.", color = NCMuted, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { showWeightEditor = true }, modifier = Modifier.fillMaxWidth()) { Text("Log / edit weight") }
        if (weights.isNotEmpty()) {
            Text("Recent weights", style = MaterialTheme.typography.titleSmall)
            weights.take(5).forEach { Text("${it.date}: ${it.weightKg} kg", color = NCMuted) }
        }
    }

    if (showNutritionEditor) NutritionEditor(day, editItem, onDismiss = { showNutritionEditor = false }, onSave = { item ->
        scope.launch { dao.saveNutrition(item) }; showNutritionEditor = false
    })
    deleting?.let { target -> ConfirmDeleteDialog(target.food, { deleting = null }, {
        scope.launch { dao.deleteNutrition(target) }; deleting = null
    }) }
    if (showBudgetEditor) BudgetEditor(
        date = day, budget = savedBudget, effectiveCalories = budget, effectiveProtein = proteinGoal,
        maxBudget = settings.maxBudget, onDismiss = { showBudgetEditor = false },
        onSave = { updated -> scope.launch { dao.saveBudget(updated) }; showBudgetEditor = false }
    )
    if (showActivityEditor) ActivityEditor(activity ?: ActivityLog(day), onDismiss = { showActivityEditor = false }, onSave = {
        scope.launch { dao.saveActivity(it) }; showActivityEditor = false
    })
    if (showWeightEditor) WeightEditor(day, weights.firstOrNull { it.date == day }, onDismiss = { showWeightEditor = false }, onSave = {
        scope.launch { dao.saveWeight(it) }; showWeightEditor = false
    })
}

private fun displayTime(millis: Long): String = Instant.ofEpochMilli(millis)
    .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))

@Composable
private fun NutritionEditor(date: String, current: NutritionEntry?, onDismiss: () -> Unit, onSave: (NutritionEntry) -> Unit) {
    var food by remember(current) { mutableStateOf(current?.food ?: "Food") }
    var calories by remember(current) { mutableStateOf(current?.calories?.toString() ?: "") }
    var protein by remember(current) { mutableStateOf(current?.protein?.toString() ?: "") }
    val cal = calories.toIntOrNull()
    val pro = protein.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(if (current == null) "Add food" else "Edit food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput("Food name", food, { food = it })
                TextInput("Calories (kcal)", calories, { calories = it })
                TextInput("Protein (grams)", protein, { protein = it })
                Text("Updated time is recorded automatically.", color = NCMuted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = food.isNotBlank() && cal != null && cal >= 0 && pro != null && pro >= 0,
                onClick = { onSave((current ?: NutritionEntry(date = date, calories = 0, protein = 0.0)).copy(
                    timestamp = System.currentTimeMillis(), food = food.trim(), calories = cal!!, protein = pro!!
                )) }
            ) { Text("Save") }
        }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BudgetEditor(
    date: String, budget: DailyBudget?, effectiveCalories: Int, effectiveProtein: Double,
    maxBudget: Int, onDismiss: () -> Unit, onSave: (DailyBudget) -> Unit
) {
    var calories by remember(date) { mutableStateOf((budget?.calorieOverride ?: effectiveCalories).toString()) }
    var protein by remember(date) { mutableStateOf((budget?.proteinOverride ?: effectiveProtein).toString()) }
    val cal = calories.toIntOrNull()
    val pro = protein.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Daily targets: $date") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput("Calorie budget (0–$maxBudget)", calories, { calories = it })
            TextInput("Protein target (grams)", protein, { protein = it })
            Text("These values override the automatic rollover for this date.", color = NCMuted, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(enabled = cal != null && cal in 0..maxBudget && pro != null && pro >= 0,
            onClick = { onSave((budget ?: DailyBudget(date)).copy(calorieOverride = cal, proteinOverride = pro)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ActivityEditor(current: ActivityLog, onDismiss: () -> Unit, onSave: (ActivityLog) -> Unit) {
    var steps by remember(current) { mutableStateOf(current.steps.toString()) }
    var pushups by remember(current) { mutableStateOf(current.pushups.toString()) }
    val stepValue = steps.toIntOrNull()
    val pushValue = pushups.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Update activity • ${current.date}") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput("Steps today", steps, { steps = it })
            TextInput("Push-ups today", pushups, { pushups = it })
        } },
        confirmButton = { TextButton(enabled = stepValue != null && stepValue >= 0 && pushValue != null && pushValue >= 0,
            onClick = { onSave(current.copy(steps = stepValue!!, pushups = pushValue!!, updatedAt = System.currentTimeMillis())) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun WeightEditor(date: String, existing: WeightLog?, onDismiss: () -> Unit, onSave: (WeightLog) -> Unit) {
    var weight by remember(date) { mutableStateOf(existing?.weightKg?.toString() ?: "") }
    val parsed = weight.toDoubleOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Weight • $date") },
        text = { TextInput("Weight in kg", weight, { weight = it }) },
        confirmButton = { TextButton(enabled = parsed != null && parsed > 0,
            onClick = { onSave(WeightLog(date, parsed!!)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
