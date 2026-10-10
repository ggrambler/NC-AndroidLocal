package com.divyansh.nc.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BudgetMathTest {
    private val config = NCSettings(baseCalories = 1600, maxBudget = 2500)

    @Test fun rollsForwardAndBack() {
        val food = listOf(
            NutritionEntry(date = "2026-10-01", calories = 1500, protein = 55.0),
            NutritionEntry(date = "2026-10-02", calories = 1850, protein = 70.0)
        )
        assertEquals(1600, BudgetMath.effective(LocalDate.parse("2026-10-01"), config, food, emptyList()))
        assertEquals(1700, BudgetMath.effective(LocalDate.parse("2026-10-02"), config, food, emptyList()))
        assertEquals(1450, BudgetMath.effective(LocalDate.parse("2026-10-03"), config, food, emptyList()))
    }

    @Test fun missingDayDoesNotCountAsZeroIntake() {
        val food = listOf(NutritionEntry(date = "2026-10-01", calories = 1500, protein = 55.0))
        assertEquals(1600, BudgetMath.effective(LocalDate.parse("2026-10-03"), config, food, emptyList()))
    }

    @Test fun fullyLoggedZeroIntakeIsCapped() {
        val budgets = listOf(DailyBudget("2026-10-01", completed = true))
        assertEquals(2500, BudgetMath.effective(LocalDate.parse("2026-10-02"), config, emptyList(), budgets))
    }

    @Test fun manualOverrideWins() {
        val override = listOf(DailyBudget("2026-10-02", calorieOverride = 1900))
        assertEquals(1900, BudgetMath.effective(LocalDate.parse("2026-10-02"), config, emptyList(), override))
    }
}
