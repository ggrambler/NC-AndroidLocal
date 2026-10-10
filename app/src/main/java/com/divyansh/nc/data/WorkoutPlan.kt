package com.divyansh.nc.data

import java.time.DayOfWeek

data class ExerciseTemplate(
    val name: String,
    val sets: Int,
    val reps: String,
    val initialKg: Double? = null
)

data class DailyWorkout(val title: String, val exercises: List<ExerciseTemplate>)

/** Starting plan from the user's UNO/DOS routine; targets are editable in a later version. */
object WorkoutPlan {
    fun forDay(day: DayOfWeek): DailyWorkout = when (day) {
        DayOfWeek.MONDAY -> DailyWorkout("UNO 1 — Push", listOf(
            ExerciseTemplate("Barbell Bench Press", 3, "6–8", 50.0),
            ExerciseTemplate("Incline DB Press", 3, "8–10"),
            ExerciseTemplate("DB Fly", 2, "10–12", 10.0),
            ExerciseTemplate("DB Lateral Raise", 3, "12–15"),
            ExerciseTemplate("Cable Triceps Pushdown", 3, "10–12", 25.0)))
        DayOfWeek.TUESDAY -> DailyWorkout("UNO 2 — Pull", listOf(
            ExerciseTemplate("Lat Pulldown", 3, "8–10", 40.0),
            ExerciseTemplate("Chest-Supported DB Row", 3, "8–12"),
            ExerciseTemplate("Straight-Arm Cable Pulldown", 2, "12–15"),
            ExerciseTemplate("Face Pull", 3, "12–15"),
            ExerciseTemplate("EZ-Bar Curl", 3, "8–12"),
            ExerciseTemplate("Hammer Curl", 2, "10–12")))
        DayOfWeek.WEDNESDAY -> DailyWorkout("UNO 3 — Legs + Core", listOf(
            ExerciseTemplate("Bodyweight Box Squat", 3, "15–20", 0.0),
            ExerciseTemplate("Supported Bulgarian Split Squat", 3, "8–10/leg", 0.0),
            ExerciseTemplate("DB Glute Bridge", 3, "10–15"),
            ExerciseTemplate("Standing Calf Raise", 3, "15–20", 0.0),
            ExerciseTemplate("Cable Crunch", 3, "10–15"),
            ExerciseTemplate("Plank (seconds as reps)", 2, "30–60 sec", 0.0)))
        DayOfWeek.THURSDAY -> DailyWorkout("DOS 1 — Chest + Back", listOf(
            ExerciseTemplate("Barbell Bench Press", 3, "8–10"),
            ExerciseTemplate("Neutral/Close-Grip Lat Pulldown", 3, "8–12"),
            ExerciseTemplate("Single-Arm Cable Row", 3, "10–12/side"),
            ExerciseTemplate("Cable Fly", 2, "12–15"),
            ExerciseTemplate("Single-Arm Lat Pulldown", 2, "12–15/side")))
        DayOfWeek.FRIDAY -> DailyWorkout("DOS 2 — Shoulders + Arms", listOf(
            ExerciseTemplate("Seated DB Shoulder Press", 3, "8–12"),
            ExerciseTemplate("Cable Lateral Raise", 3, "12–15"),
            ExerciseTemplate("Rear-Delt Cable Fly", 3, "12–15"),
            ExerciseTemplate("Cable Triceps Pushdown", 3, "10–15"),
            ExerciseTemplate("Incline DB Curl", 3, "10–12"),
            ExerciseTemplate("Overhead Cable Triceps Extension", 2, "10–15")))
        DayOfWeek.SATURDAY -> DailyWorkout("DOS 3 — Legs + Core", listOf(
            ExerciseTemplate("Bodyweight Squat", 3, "15–20", 0.0),
            ExerciseTemplate("Reverse Lunge / Supported Split Squat", 3, "8–12/leg", 0.0),
            ExerciseTemplate("DB Hip Thrust", 3, "10–15"),
            ExerciseTemplate("Hamstring Walkout", 3, "8–12", 0.0),
            ExerciseTemplate("Standing Calf Raise", 3, "15–25", 0.0),
            ExerciseTemplate("Pallof Press", 3, "10–15/side")))
        DayOfWeek.SUNDAY -> DailyWorkout("DOS 4 — Upper Power", listOf(
            ExerciseTemplate("Barbell Bench Press", 3, "5–6"),
            ExerciseTemplate("Lat Pulldown", 3, "6–8"),
            ExerciseTemplate("Chest-Supported DB Row", 3, "8–10"),
            ExerciseTemplate("DB Lateral Raise", 2, "12–15"),
            ExerciseTemplate("EZ-Bar Curl", 2, "8–10"),
            ExerciseTemplate("Rope Triceps Pushdown", 2, "10–12")))
    }
}
