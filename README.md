# NC — Nutrition & Conditioning (Android)

An offline-first personal fitness tracker built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room**, **WorkManager**, and Android **MediaStore**. All records live on the phone, with CSV report copies in `Downloads/NCREPORT/`.

## Install into your EXISTING Android Studio project

**Your working directory:** `C:\Users\DIVYA\AndroidStudioProjects\NC`

1. Commit your working UI first: `git add . && git commit -m "chore: save prior working navigation"`.
2. Create a branch: `git switch -c feat/full-nc-mvp`.
3. Unzip this archive. **Copy the archive's contents** into the root of your existing `NC` project, replacing matching files. In particular, use the included `app/build.gradle.kts`, root `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `app/src/main` sources, and `gradle/wrapper/gradle-wrapper.properties`.
4. **Keep your existing** `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`. They aren't included in this archive because Android Studio already generated them for you. Keep your `.git/` directory too.
5. Android Studio → **File → Sync Project with Gradle Files** (shortcut `Ctrl+Shift+A`, search for Sync). Install Android SDK 36 if prompted.
6. Run the `app` configuration on an Android **10+** device/emulator. First launch asks your name, height and optional weight.
7. After a successful build: `git add .`, `git commit -m "feat: implement NC offline fitness MVP"`, `git push -u origin feat/full-nc-mvp`.
8. Review the pull request, merge to `main`. The GitHub Actions workflow will build/test the app on every push.

**Do not replace the project folder wholesale**, and don't delete `.git`. This archive is a source overlay for the Android Studio project you already initialized.

## Screens

- **References:** create, rename, delete meal-reference plans; import CSV using Android system file picker; independently add/edit/delete foods; reference-only totals for calories/protein/fibre. These references **never change nutrition**.
- **Nutrition:** independent daily calorie budget and protein goal; animated concentric rings, dated CRUD food logs, timestamped edits, daily manual budget override and calorie rollover.
- **Training:** your seven-day UNO/DOS templates. Save/edit/remove individual sets, prefill last session's reps/weight, see previous history, best logged weight and progression prompt.
- **Daily extras:** manual steps and push-ups, goals and evening check-in; dated weight log and calculated BMI.
- **Hamburger menu:** configurable profile/height, calorie cap, rollover toggle, protein target, step/push-up goals and reminder time; manual complete CSV export.

## Reports

Reports are written via Android MediaStore to **Downloads/NCREPORT/**. Examples:

```
Downloads/NCREPORT/
├── References/1_CuttingPlan.csv
├── Nutrition/2026-Q4/101026_nutrition.csv
├── Training/2026-Q4/101026_workout.csv
├── Activity/2026-Q4/101026_activity.csv
└── Weight/weight_history.csv
```

The app maintains **Room SQLite data as its source of truth**. Daily CSV files are *snapshots*, not the database. CSVs are quoted and include timestamps; `budget.csv` includes the effective budget/consumption. **Changes made after the nightly job require manual re-export** to ensure a current CSV. The 90-day archive cycle is modeled as calendar quarters (`YYYY-Q1` etc.), not a rolling 90-day window. The app reminds you during the first week of a new quarter to archive previous files.

### Meal reference import

See `samples/reference_plan.csv`. Accepted names include `food`/`food name`/`name`, `amount`/`amt`, `calories`, `protein`, `fibre`/`fiber`. The header needs at least a food-name and calories column. One food per line; quoted commas work, multiline CSV cells are not supported in this MVP.

## Calorie rollover

- Day's target = (default daily budget + previous *confirmed/logged* day calorie difference), clamped between **0 and Settings maximum (2500 initially)**.
- An explicit manually set daily calorie budget overrides the formula for that date; you can correct missed dates. Historical corrections recompute later dates when viewing them.
- A completely **unlogged day breaks the rollover chain**: it is not treated as 0 calories eaten. To mark an actual zero-entry day as complete, select **Mark this day fully logged**.
- Protein does not roll over.
- A displayed **0 calorie budget is an accounting value, not an instruction to fast**. You can edit the day's target; very restrictive intake should not be used as compensation for overeating.

## Android reminders and battery

- Evening reminder: around **22:00** by default; checks whether manually entered steps/push-ups meet your configured minima and asks you to review entries.
- CSV job: around **23:58**, exports the current and previous day's nutrition/workout/activity and reference/weight CSVs.
- Both use **WorkManager**. Android can delay jobs under Doze/battery constraints. **These are NOT guaranteed to execute at the exact displayed minute**. Opening the app and choosing Export all gives an immediate on-demand report.
- Permission requested: `POST_NOTIFICATIONS` on Android 13+. **No permanent foreground service, battery optimization bypass, broad storage permission, account, location, or internet required.**
- Steps are **entered manually in this version**, not measured automatically by your phone's sensor. Native step sensing in the background is a future integration that needs additional runtime permissions/platform-specific handling.

## Structure

```
app/src/main/java/com/divyansh/nc/
  MainActivity.kt                Compose shell, navigation, onboarding
  data/NCData.kt                Room entities/DAO/database and rollover rules
  data/WorkoutPlan.kt           Seven-day UNO/DOS exercises
  ui/NCTheme.kt                 Palette and Material3 theme
  ui/NCComponents.kt            Shared UI, animated concentric rings
  ui/ReferencesScreen.kt        Meal-reference editing/import
  ui/NutritionScreen.kt         Daily budget/food, steps, push-ups, weight/BMI
  ui/TrainingScreen.kt          Per-set exercise logging/history
  ui/SettingsScreen.kt          Configuration
  report/NCReports.kt           CSV import/export to scoped Downloads
  background/NCBackground.kt    WorkManager jobs and notifications
```

## Known MVP limitations / TODO

1. Requires Android 10+ for straightforward scoped MediaStore downloads. The original project may have had minSdk 26; this package intentionally raises it to 29.
2. Step counts require manual entry; no continuous sensor listener/Health Connect yet.
3. Notification/export times are approximate due to WorkManager; users must grant notifications permission.
4. Exercise templates are hardcoded starting values; workouts can be logged, but a workout-template editor is not yet built.
5. No encrypted backups, database restore/import of nutrition/workout reports, automatic cloud sync, charts for weight history, 1RM testing, or body-fat estimation.
6. Not yet verified through an actual Android SDK build in the packaging environment. Gradle Sync and compile/test on your Android Studio installation are required; report the first compiler error if one arises.
7. `NCReports.importPlan()` skips invalid CSV rows; review imported plans, especially numeric food values.

## Verify

From Android Studio terminal:

```bash
./gradlew testDebugUnitTest assembleDebug  # Git Bash/macOS/Linux
# Or on Windows Command Prompt:
gradlew.bat testDebugUnitTest assembleDebug
```

Expected APK: `app/build/outputs/apk/debug/app-debug.apk`.

Recommended test: import sample plan; confirm Reference total does NOT alter Nutrition; add food and edit/deletion; manually modify a prior date budget and check rollover; log sets and verify prefill on a later matching workout; edit steps/weight; export and inspect CSVs in Files → Downloads → NCREPORT.
