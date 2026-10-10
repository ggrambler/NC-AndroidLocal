package com.divyansh.nc.report

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.divyansh.nc.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Saves CSV into public Downloads/NCREPORT using scoped storage; Android 10+ only. */
class NCReports(private val context: Context, private val dao: NCDao) {
    private val stamp = DateTimeFormatter.ofPattern("ddMMyy")
    private val instantStamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private fun escape(value: Any?): String = "\"" + (value?.toString() ?: "").replace("\"", "\"\"") + "\""
    private fun csv(headers: List<String>, rows: List<List<Any?>>): String = buildString {
        appendLine(headers.joinToString(",") { escape(it) })
        rows.forEach { appendLine(it.joinToString(",") { v -> escape(v) }) }
    }
    private fun timestamp(ms: Long): String = java.time.Instant.ofEpochMilli(ms)
        .atZone(ZoneId.systemDefault()).toLocalDateTime().format(instantStamp)

    private fun quarter(date: LocalDate): String = "${date.year}-Q${((date.monthValue - 1) / 3) + 1}"

    private fun write(folder: String, filename: String, text: String) {
        val resolver = context.contentResolver
        val relative = "${Environment.DIRECTORY_DOWNLOADS}/NCREPORT/$folder/"
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        var uri: Uri? = null
        resolver.query(
            collection, arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
            arrayOf(filename, relative), null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                uri = android.content.ContentUris.withAppendedId(collection, cursor.getLong(0))
            }
        }
        val destination = uri ?: run {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relative)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            resolver.insert(collection, values) ?: error("Failed to create Downloads/$relative$filename")
        }
        try {
            resolver.openOutputStream(destination, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
                ?: error("Unable to open CSV for writing: $filename")
            resolver.update(destination, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.update(destination, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            throw e
        }
    }

    suspend fun exportDay(day: LocalDate) = withContext(Dispatchers.IO) {
        val date = day.toString()
        val folder = quarter(day)
        val nutrition = dao.getNutrition(date)
        val workouts = dao.getSets(date)
        val budget = dao.getBudget(date)
        val settings = dao.getSettings() ?: NCSettings()
        val allNutrition = dao.getAllNutrition()
        val allBudgets = dao.getAllBudgets()
        val effectiveBudget = BudgetMath.effective(day, settings, allNutrition, allBudgets)
        val effectiveProtein = BudgetMath.proteinGoal(date, settings, allBudgets)
        val activity = dao.getActivity(date)
        val summary = listOf<Any?>(effectiveBudget, effectiveProtein,
            nutrition.sumOf { it.calories }, nutrition.sumOf { it.protein },
            effectiveBudget - nutrition.sumOf { it.calories }, budget?.calorieOverride,
            budget?.proteinOverride, budget?.completed ?: false, settings.baseCalories,
            settings.maxBudget, settings.carryoverEnabled, timestamp(System.currentTimeMillis()))
        val nutritionRows = if (nutrition.isEmpty()) {
            listOf(listOf<Any?>(null, date, null, null, null, null) + summary)
        } else nutrition.map {
            listOf<Any?>(it.id, it.date, timestamp(it.timestamp), it.food, it.calories, it.protein) + summary
        }
        write("Nutrition/$folder", "${day.format(stamp)}_nutrition.csv", csv(
            listOf("entry_id", "date", "updated_at", "food", "calories", "protein_g",
                "effective_budget", "protein_target_g", "consumed_calories", "consumed_protein_g",
                "calories_remaining", "budget_override", "protein_override", "completed",
                "base_calories", "max_budget", "rollover_on", "report_generated_at"), nutritionRows
        ))
        write("Training/$folder", "${day.format(stamp)}_workout.csv", csv(
            listOf("id", "date", "updated_at", "exercise", "set", "reps", "weight_kg"),
            workouts.map { listOf(it.id, it.date, timestamp(it.timestamp), it.exercise, it.setNumber, it.reps, it.weightKg) }
        ))
        write("Activity/$folder", "${day.format(stamp)}_activity.csv", csv(
            listOf("date", "steps", "pushups", "updated_at"),
            if (activity == null) emptyList() else listOf(listOf(activity.date, activity.steps, activity.pushups, timestamp(activity.updatedAt)))
        ))

    }

    suspend fun exportReferences() = withContext(Dispatchers.IO) {
        dao.getPlans().forEach { plan ->
            val safe = plan.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(42)
            write("References", "${plan.id}_${safe}.csv", csv(
                listOf("food", "amount", "calories", "protein", "fibre"),
                dao.getFoods(plan.id).map { listOf(it.food, it.amount, it.calories, it.protein, it.fibre) }
            ))
        }
        write("Weight", "weight_history.csv", csv(
            listOf("date", "timestamp", "weight_kg"),
            dao.getWeights().map { listOf(it.date, timestamp(it.timestamp), it.weightKg) }
        ))
    }

    suspend fun exportEverything() = withContext(Dispatchers.IO) {
        val dates = (dao.recordedDates() + LocalDate.now().toString()).distinct()
        dates.forEach { exportDay(LocalDate.parse(it)) }
        exportReferences()
    }

    /** Validates a simple CSV with quoted fields; embedded newlines are not supported in v1. */
    private fun splitCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> { field.append('"'); i++ }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { cells.add(field.toString().trim()); field.clear() }
                else -> field.append(c)
            }
            i++
        }
        cells.add(field.toString().trim())
        return cells
    }

    suspend fun importPlan(uri: Uri, name: String): Int = withContext(Dispatchers.IO) {
        val lines = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readLines() }
            ?: error("Cannot open selected CSV")
        require(lines.isNotEmpty()) { "CSV is empty" }
        val header = splitCsvLine(lines.first()).map { it.lowercase().replace(" ", "").replace("_", "") }
        fun index(vararg aliases: String): Int = aliases.firstNotNullOfOrNull { alias ->
            header.indexOf(alias).takeIf { it >= 0 }
        } ?: -1
        val nameIndex = index("food", "foodname", "name")
        val calIndex = index("calories", "kcal", "calorie")
        require(nameIndex >= 0 && calIndex >= 0) { "CSV must contain food/name and calories columns" }
        val amountIndex = index("amount", "amt", "quantity")
        val proteinIndex = index("protein", "proteing", "proteins")
        val fibreIndex = index("fibre", "fiber", "fibreg", "fiberg")
        val parsed = lines.drop(1).filter { it.isNotBlank() }.mapNotNull { line ->
            val cells = splitCsvLine(line)
            fun cell(i: Int) = if (i < 0) "" else cells.getOrElse(i) { "" }
            val food = cell(nameIndex)
            val calories = cell(calIndex).toDoubleOrNull()?.toInt()
            if (food.isBlank() || calories == null) null else ReferenceFood(
                planId = 0, food = food,
                amount = cell(amountIndex), calories = calories.coerceAtLeast(0),
                protein = (cell(proteinIndex).toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                fibre = (cell(fibreIndex).toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
            )
        }
        require(parsed.isNotEmpty()) { "No valid food rows in CSV" }
        val planId = dao.savePlan(ReferencePlan(title = name.ifBlank { "Imported plan" }))
        parsed.forEach { dao.saveFood(it.copy(planId = planId)) }
        parsed.size
    }
}
