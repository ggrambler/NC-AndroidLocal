package com.divyansh.nc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divyansh.nc.data.NCSettings

@Composable
fun SettingsScreen(current: NCSettings, onSave: (NCSettings) -> Unit, onExport: () -> Unit) {
    var name by remember(current) { mutableStateOf(current.name) }
    var height by remember(current) { mutableStateOf(current.heightCm.toString()) }
    var base by remember(current) { mutableStateOf(current.baseCalories.toString()) }
    var protein by remember(current) { mutableStateOf(current.proteinGoal.toString()) }
    var cap by remember(current) { mutableStateOf(current.maxBudget.toString()) }
    var carryover by remember(current) { mutableStateOf(current.carryoverEnabled) }
    var stepGoal by remember(current) { mutableStateOf(current.stepGoal.toString()) }
    var pushupGoal by remember(current) { mutableStateOf(current.pushupGoal.toString()) }
    var reminderHour by remember(current) { mutableStateOf(current.reminderHour.toString()) }
    var reminderMinute by remember(current) { mutableStateOf(current.reminderMinute.toString()) }

    SectionTitle("Settings", "Profile, daily goals, carryover and local CSV reports")
    NCCard {
        Text("Your profile", style = MaterialTheme.typography.titleMedium)
        TextInput("Display name", name, { name = it })
        TextInput("Height (cm)", height, { height = it })
    }
    NCCard {
        Text("Nutrition settings", style = MaterialTheme.typography.titleMedium)
        TextInput("Default calories/day", base, { base = it })
        TextInput("Protein target (grams)", protein, { protein = it })
        TextInput("Maximum allowed daily budget", cap, { cap = it })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = carryover, onCheckedChange = { carryover = it })
            Spacer(Modifier.width(12.dp))
            Text("Automatically carry calorie difference to next logged day")
        }
        Text("Rollover never makes tomorrow's budget negative. Unlogged days reset to base targets.",
            color = NCMuted, style = MaterialTheme.typography.bodySmall)
    }
    NCCard {
        Text("Activity and reminders", style = MaterialTheme.typography.titleMedium)
        TextInput("Daily step goal", stepGoal, { stepGoal = it })
        TextInput("Daily push-up goal", pushupGoal, { pushupGoal = it })
        TextInput("Evening reminder hour (0–23)", reminderHour, { reminderHour = it })
        TextInput("Evening reminder minute (0–59)", reminderMinute, { reminderMinute = it })
        Text("CSV export aims for 23:58 daily. Android can delay background work for battery optimization.",
            color = NCMuted, style = MaterialTheme.typography.bodySmall)
    }
    val heightValue = height.toDoubleOrNull()
    val baseValue = base.toIntOrNull()
    val proteinValue = protein.toDoubleOrNull()
    val capValue = cap.toIntOrNull()
    val stepsValue = stepGoal.toIntOrNull()
    val pushupsValue = pushupGoal.toIntOrNull()
    val hourValue = reminderHour.toIntOrNull()
    val minuteValue = reminderMinute.toIntOrNull()
    val valid = name.isNotBlank() && heightValue != null && heightValue > 0 &&
        baseValue != null && baseValue >= 0 && proteinValue != null && proteinValue >= 0 &&
        capValue != null && capValue >= baseValue && stepsValue != null && stepsValue >= 0 &&
        pushupsValue != null && pushupsValue >= 0 && hourValue != null && hourValue in 0..23 &&
        minuteValue != null && minuteValue in 0..59
    Button(enabled = valid, modifier = Modifier.fillMaxWidth(), onClick = {
        onSave(current.copy(
            name = name.trim(), heightCm = heightValue!!,
            baseCalories = baseValue!!, proteinGoal = proteinValue!!, maxBudget = capValue!!,
            carryoverEnabled = carryover, stepGoal = stepsValue!!, pushupGoal = pushupsValue!!,
            reminderHour = hourValue!!, reminderMinute = minuteValue!!
        ))
    }) { Text("Save settings") }
    OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export all CSV files now") }
    Text("Files: Downloads/NCREPORT/ · Reports are grouped into calendar quarters.",
        color = NCMuted, style = MaterialTheme.typography.bodySmall)
    Text("NC does not need to run permanently. Reminders use Android WorkManager and notifications need user permission.",
        color = NCMuted, style = MaterialTheme.typography.bodySmall)
}
