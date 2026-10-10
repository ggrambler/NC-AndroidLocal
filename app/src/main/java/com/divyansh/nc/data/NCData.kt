package com.divyansh.nc.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.math.roundToInt

@Entity(tableName = "app_settings")
data class NCSettings(
    @PrimaryKey val id: Int = 1,
    val initialized: Boolean = false,
    val name: String = "Divyansh",
    val heightCm: Double = 173.0,
    val baseCalories: Int = 1600,
    val proteinGoal: Double = 110.0,
    val maxBudget: Int = 2500,
    val carryoverEnabled: Boolean = true,
    val stepGoal: Int = 10000,
    val pushupGoal: Int = 50,
    val reminderHour: Int = 22,
    val reminderMinute: Int = 0
)

@Entity(tableName = "daily_budgets")
data class DailyBudget(
    @PrimaryKey val date: String,
    val calorieOverride: Int? = null,
    val proteinOverride: Double? = null,
    val completed: Boolean = false
)

@Entity(tableName = "nutrition_entries", indices = [Index(value = ["date"])])
data class NutritionEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val food: String = "Food",
    val calories: Int,
    val protein: Double
)

@Entity(tableName = "reference_plans")
data class ReferencePlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reference_foods",
    foreignKeys = [ForeignKey(entity = ReferencePlan::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("planId")]
)
data class ReferenceFood(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val food: String,
    val amount: String,
    val calories: Int,
    val protein: Double,
    val fibre: Double
)

@Entity(tableName = "workout_sets", indices = [Index(value = ["date", "exercise", "setNumber"], unique = true)])
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val exercise: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey val date: String,
    val steps: Int = 0,
    val pushups: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey val date: String,
    val weightKg: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface NCDao {
    @Query("SELECT * FROM app_settings WHERE id = 1") fun settings(): Flow<NCSettings?>
    @Query("SELECT * FROM app_settings WHERE id = 1") suspend fun getSettings(): NCSettings?
    @Upsert suspend fun saveSettings(item: NCSettings)

    @Query("SELECT * FROM daily_budgets ORDER BY date") fun budgets(): Flow<List<DailyBudget>>
    @Query("SELECT * FROM daily_budgets ORDER BY date") suspend fun getAllBudgets(): List<DailyBudget>
    @Query("SELECT * FROM daily_budgets WHERE date = :date") suspend fun getBudget(date: String): DailyBudget?
    @Upsert suspend fun saveBudget(item: DailyBudget)

    @Query("SELECT * FROM nutrition_entries ORDER BY timestamp DESC") fun nutrition(): Flow<List<NutritionEntry>>
    @Query("SELECT * FROM nutrition_entries ORDER BY timestamp DESC") suspend fun getAllNutrition(): List<NutritionEntry>
    @Query("SELECT * FROM nutrition_entries WHERE date = :date ORDER BY timestamp") suspend fun getNutrition(date: String): List<NutritionEntry>
    @Upsert suspend fun saveNutrition(item: NutritionEntry)
    @Delete suspend fun deleteNutrition(item: NutritionEntry)

    @Query("SELECT * FROM reference_plans ORDER BY createdAt DESC") fun plans(): Flow<List<ReferencePlan>>
    @Query("SELECT * FROM reference_plans ORDER BY createdAt DESC") suspend fun getPlans(): List<ReferencePlan>
    @Upsert suspend fun savePlan(item: ReferencePlan): Long
    @Delete suspend fun deletePlan(item: ReferencePlan)
    @Query("SELECT * FROM reference_foods ORDER BY id") fun foods(): Flow<List<ReferenceFood>>
    @Query("SELECT * FROM reference_foods WHERE planId = :planId ORDER BY id") suspend fun getFoods(planId: Long): List<ReferenceFood>
    @Upsert suspend fun saveFood(item: ReferenceFood)
    @Delete suspend fun deleteFood(item: ReferenceFood)

    @Query("SELECT * FROM workout_sets ORDER BY date DESC, exercise, setNumber") fun sets(): Flow<List<WorkoutSet>>
    @Query("SELECT * FROM workout_sets WHERE date = :date ORDER BY exercise, setNumber") suspend fun getSets(date: String): List<WorkoutSet>
    @Upsert suspend fun saveSet(item: WorkoutSet)
    @Delete suspend fun deleteSet(item: WorkoutSet)

    @Query("SELECT * FROM activity_logs ORDER BY date DESC") fun activity(): Flow<List<ActivityLog>>
    @Query("SELECT * FROM activity_logs WHERE date = :date") suspend fun getActivity(date: String): ActivityLog?
    @Upsert suspend fun saveActivity(item: ActivityLog)

    @Query("SELECT * FROM weight_logs ORDER BY date DESC") fun weights(): Flow<List<WeightLog>>
    @Upsert suspend fun saveWeight(item: WeightLog)
    @Delete suspend fun deleteWeight(item: WeightLog)
    @Query("SELECT * FROM weight_logs ORDER BY date DESC") suspend fun getWeights(): List<WeightLog>

    @Query("SELECT DISTINCT date FROM nutrition_entries UNION SELECT date FROM workout_sets UNION SELECT date FROM daily_budgets UNION SELECT date FROM activity_logs UNION SELECT date FROM weight_logs ORDER BY date")
    suspend fun recordedDates(): List<String>
}

@Database(
    entities = [NCSettings::class, DailyBudget::class, NutritionEntry::class,
        ReferencePlan::class, ReferenceFood::class, WorkoutSet::class,
        ActivityLog::class, WeightLog::class],
    version = 1,
    exportSchema = false
)
abstract class NCDatabase : RoomDatabase() {
    abstract fun dao(): NCDao
    companion object {
        @Volatile private var instance: NCDatabase? = null
        fun get(context: Context): NCDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NCDatabase::class.java, "nc.db")
                .build().also { instance = it }
        }
    }
}

/** Pure calculation: references are never used. Missing/unconfirmed dates reset rollover. */
object BudgetMath {
    fun effective(
        day: LocalDate,
        settings: NCSettings,
        entries: List<NutritionEntry>,
        budgets: List<DailyBudget>
    ): Int {
        val entryDays = entries.groupBy { it.date }
        val overrides = budgets.associateBy { it.date }
        val dates = (entries.map { it.date } + budgets.map { it.date })
            .filter { it <= day.toString() }
        val first = dates.minOrNull()?.let(LocalDate::parse) ?: day
        var previous = first.minusDays(1)
        var amount = settings.baseCalories.coerceIn(0, settings.maxBudget.coerceAtLeast(0))
        var cursor = first
        while (!cursor.isAfter(day)) {
            val key = cursor.toString()
            val manual = overrides[key]?.calorieOverride
            amount = when {
                manual != null -> manual.coerceIn(0, settings.maxBudget.coerceAtLeast(0))
                !settings.carryoverEnabled -> settings.baseCalories
                cursor == first -> settings.baseCalories
                entryDays[previous.toString()].isNullOrEmpty() && overrides[previous.toString()]?.completed != true -> settings.baseCalories
                else -> (settings.baseCalories + amount - (entryDays[previous.toString()]?.sumOf { it.calories } ?: 0))
            }.coerceIn(0, settings.maxBudget.coerceAtLeast(0))
            previous = cursor
            cursor = cursor.plusDays(1)
        }
        return amount
    }

    fun proteinGoal(date: String, settings: NCSettings, budgets: List<DailyBudget>): Double =
        budgets.firstOrNull { it.date == date }?.proteinOverride ?: settings.proteinGoal

    fun bmi(weightKg: Double, heightCm: Double): Double? {
        if (weightKg <= 0 || heightCm <= 0) return null
        val meters = heightCm / 100.0
        return weightKg / (meters * meters)
    }

    fun completionPercent(consumed: Number, target: Number): Int =
        if (target.toDouble() <= 0.0) 0 else ((consumed.toDouble() / target.toDouble()) * 100).roundToInt()
}
