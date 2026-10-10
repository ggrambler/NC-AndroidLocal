package com.divyansh.nc.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.divyansh.nc.data.*
import com.divyansh.nc.report.NCReports
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ReferencesScreen(
    plans: List<ReferencePlan>, foods: List<ReferenceFood>, dao: NCDao,
    notify: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPlanId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(plans) {
        if (plans.none { it.id == selectedPlanId }) selectedPlanId = plans.firstOrNull()?.id
    }
    val current = plans.firstOrNull { it.id == selectedPlanId }
    val rows = foods.filter { it.planId == selectedPlanId }
    var planEditor by remember { mutableStateOf<ReferencePlan?>(null) }
    var addPlan by remember { mutableStateOf(false) }
    var deletePlan by remember { mutableStateOf(false) }
    var editFood by remember { mutableStateOf<ReferenceFood?>(null) }
    var addFood by remember { mutableStateOf(false) }
    var deleteFood by remember { mutableStateOf<ReferenceFood?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching { NCReports(context, dao).importPlan(uri, "Meal reference ${plans.size + 1}") }
            notify(result.fold({ "Imported $it reference foods" }, { "Import error: ${it.message}" }))
        }
    }

    SectionTitle("Food References", "Editable CSV-based reference plans. Completely separate from your daily intake.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { picker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) }, modifier = Modifier.weight(1f)) {
            Text("Import CSV")
        }
        OutlinedButton(onClick = { addPlan = true; planEditor = null }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Add, null)
            Text("New plan")
        }
    }
    if (plans.isNotEmpty()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            plans.forEach { plan ->
                FilterChip(selected = plan.id == selectedPlanId, onClick = { selectedPlanId = plan.id }, label = { Text(plan.title) })
            }
        }
    }
    if (current == null) {
        NCCard {
            Text("No reference plans yet.")
            Text("Import CSV columns: food, amount, calories, protein, fibre. Or create a blank plan.", color = NCMuted)
        }
    } else {
        NCCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(current.title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { planEditor = current; addPlan = true }) { Icon(Icons.Filled.Edit, "Rename plan") }
                IconButton(onClick = { deletePlan = true }) { Icon(Icons.Filled.Delete, "Delete plan", tint = NCRed) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("Total calories", "${rows.sumOf { it.calories }}", NCPurple, Modifier.weight(1f))
                MetricPill("Total protein", "${rows.sumOf { it.protein }.roundToInt()} g", NCGreen, Modifier.weight(1f))
            }
            Text("Total fibre: ${rows.sumOf { it.fibre }.roundToInt()}g   •   ${rows.size} foods", color = NCMuted)
            Button(onClick = { editFood = null; addFood = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null)
                Text("Add reference food")
            }
        }
        if (rows.isEmpty()) Text("Add foods to this reference plan.", color = NCMuted)
        rows.forEach { food ->
            NCCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(food.food, style = MaterialTheme.typography.titleMedium)
                        Text("${food.amount} • ${food.calories} kcal • ${food.protein}g protein • ${food.fibre}g fibre", color = NCMuted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { editFood = food; addFood = true }) { Icon(Icons.Filled.Edit, "Edit food") }
                    IconButton(onClick = { deleteFood = food }) { Icon(Icons.Filled.Delete, "Delete food", tint = NCRed) }
                }
            }
        }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
            scope.launch {
                val result = runCatching { NCReports(context, dao).exportReferences() }
                notify(result.fold({ "Reference CSV files exported" }, { "Export failed: ${it.message}" }))
            }
        }) { Text("Export reference CSV") }
    }

    if (addPlan) PlanEditor(planEditor, { addPlan = false }, { title ->
        scope.launch {
            val id = dao.savePlan((planEditor ?: ReferencePlan(title = title)).copy(title = title))
            if (planEditor == null) selectedPlanId = id
        }
        addPlan = false
    })
    if (deletePlan && current != null) ConfirmDeleteDialog(current.title, { deletePlan = false }, {
        scope.launch { dao.deletePlan(current) }; deletePlan = false
    })
    if (addFood && current != null) ReferenceFoodEditor(current.id, editFood, { addFood = false }, {
        scope.launch { dao.saveFood(it) }; addFood = false
    })
    deleteFood?.let { target -> ConfirmDeleteDialog(target.food, { deleteFood = null }, {
        scope.launch { dao.deleteFood(target) }; deleteFood = null
    }) }
}

@Composable
private fun PlanEditor(current: ReferencePlan?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var title by remember(current) { mutableStateOf(current?.title ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (current == null) "Create reference plan" else "Rename plan") },
        text = { TextInput("Plan title", title, { title = it }) },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(title.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun ReferenceFoodEditor(planId: Long, current: ReferenceFood?, onDismiss: () -> Unit, onSave: (ReferenceFood) -> Unit) {
    var name by remember(current) { mutableStateOf(current?.food ?: "") }
    var amount by remember(current) { mutableStateOf(current?.amount ?: "") }
    var calories by remember(current) { mutableStateOf(current?.calories?.toString() ?: "") }
    var protein by remember(current) { mutableStateOf(current?.protein?.toString() ?: "") }
    var fibre by remember(current) { mutableStateOf(current?.fibre?.toString() ?: "") }
    val cal = calories.toIntOrNull()
    val pro = protein.toDoubleOrNull()
    val fib = fibre.toDoubleOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (current == null) "Add food" else "Edit food") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextInput("Food name", name, { name = it })
            TextInput("Amount (e.g. 150g)", amount, { amount = it })
            TextInput("Calories", calories, { calories = it })
            TextInput("Protein (grams)", protein, { protein = it })
            TextInput("Fibre (grams)", fibre, { fibre = it })
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank() && cal != null && cal >= 0 && pro != null && pro >= 0 && fib != null && fib >= 0,
            onClick = { onSave((current ?: ReferenceFood(planId = planId, food = name, amount = amount, calories = 0, protein = 0.0, fibre = 0.0))
                .copy(food = name.trim(), amount = amount.trim(), calories = cal!!, protein = pro!!, fibre = fib!!)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
