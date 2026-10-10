package com.divyansh.nc

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divyansh.nc.background.NCWorkScheduler
import com.divyansh.nc.data.*
import com.divyansh.nc.report.NCReports
import com.divyansh.nc.ui.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class NCTab(val label: String) { REFERENCES("References"), NUTRITION("Nutrition"), TRAINING("Training") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        val db = NCDatabase.get(applicationContext)
        setContent { NCTheme { NCApp(db) } }
    }
}

@Composable
fun NCApp(database: NCDatabase) {
    val context = LocalContext.current
    val dao = database.dao()
    val scope = rememberCoroutineScope()
    val settings by dao.settings().collectAsState(initial = null)
    val entries by dao.nutrition().collectAsState(initial = emptyList())
    val budgets by dao.budgets().collectAsState(initial = emptyList())
    val plans by dao.plans().collectAsState(initial = emptyList())
    val foods by dao.foods().collectAsState(initial = emptyList())
    val sets by dao.sets().collectAsState(initial = emptyList())
    val activities by dao.activity().collectAsState(initial = emptyList())
    val weights by dao.weights().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) { if (dao.getSettings() == null) dao.saveSettings(NCSettings()) }
    val current = settings ?: return
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(current.initialized) {
        if (current.initialized) {
            NCWorkScheduler.schedule(context, current)
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (!current.initialized) {
        SetupScreen(initial = current) { name, height, weight ->
            scope.launch {
                dao.saveSettings(current.copy(name = name, heightCm = height, initialized = true))
                if (weight > 0) dao.saveWeight(WeightLog(LocalDate.now().toString(), weight))
            }
        }
        return
    }

    var selectedTabName by rememberSaveable { mutableStateOf(NCTab.NUTRITION.name) }
    val selectedTab = NCTab.valueOf(selectedTabName)
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = LocalDate.parse(selectedDate)
    var showingSettings by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }

    fun exportAll() {
        scope.launch {
            val outcome = runCatching { NCReports(context, dao).exportEverything() }
            snackbar.showSnackbar(outcome.fold(
                onSuccess = { "CSV files saved in Downloads/NCREPORT" },
                onFailure = { "Export failed: ${it.message}" }
            ))
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("NC • CONTROL CENTER", Modifier.padding(24.dp), color = NCPurple, style = MaterialTheme.typography.titleMedium)
                NavigationDrawerItem(label = { Text("Settings") }, selected = showingSettings, onClick = {
                    showingSettings = true
                    scope.launch { drawerState.close() }
                }, icon = { Icon(Icons.Filled.Settings, null) })
                NavigationDrawerItem(label = { Text("Export all CSV reports") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    exportAll()
                }, icon = { Icon(Icons.Filled.FileDownload, null) })
                Text("Stored locally • no account required", Modifier.padding(20.dp), color = NCMuted)
            }
        }
    ) {
        Scaffold(
            containerColor = NCBlack,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                Surface(color = NCBlack) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), color = NCMuted, style = MaterialTheme.typography.labelMedium)
                            Text("Hello ${current.name}!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Row {
                            if (showingSettings) IconButton(onClick = { showingSettings = false }) {
                                Icon(Icons.Filled.ArrowBack, "Back")
                            }
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, "Open settings and export menu", tint = NCPurple)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = NCSurface) {
                    NCTab.entries.forEach { tab ->
                        val icon = when (tab) {
                            NCTab.REFERENCES -> Icons.Filled.MenuBook
                            NCTab.NUTRITION -> Icons.Filled.LocalFireDepartment
                            NCTab.TRAINING -> Icons.Filled.FitnessCenter
                        }
                        NavigationBarItem(
                            selected = !showingSettings && selectedTab == tab,
                            onClick = { selectedTabName = tab.name; showingSettings = false },
                            icon = { Icon(icon, tab.label) }, label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (showingSettings) {
                    SettingsScreen(current,
                        onSave = { value ->
                            scope.launch {
                                dao.saveSettings(value)
                                NCWorkScheduler.schedule(context, value)
                                snackbar.showSnackbar("Settings saved")
                            }
                        },
                        onExport = ::exportAll)
                } else {
                    if (selectedTab != NCTab.REFERENCES) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { selectedDate = date.minusDays(1).toString() }) { Text("◀ Previous") }
                            Text(date.format(DateTimeFormatter.ofPattern("dd MMM yy")), color = NCPurple)
                            TextButton(onClick = { selectedDate = date.plusDays(1).toString() }) { Text("Next ▶") }
                        }
                    }
                    when (selectedTab) {
                        NCTab.REFERENCES -> ReferencesScreen(plans, foods, dao) { msg -> scope.launch { snackbar.showSnackbar(msg) } }
                        NCTab.NUTRITION -> NutritionScreen(date, current, entries, budgets, activities, weights, dao)
                        NCTab.TRAINING -> TrainingScreen(date, sets, dao)
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(initial: NCSettings, onFinish: (String, Double, Double) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var height by rememberSaveable { mutableStateOf(initial.heightCm.toString()) }
    var weight by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        SectionTitle("Welcome to NC", "Private, local-first fitness tracking")
        Spacer(Modifier.height(20.dp))
        TextInput("Your name", name, { name = it })
        Spacer(Modifier.height(8.dp))
        TextInput("Height (cm)", height, { height = it })
        Spacer(Modifier.height(8.dp))
        TextInput("Current weight (kg, optional)", weight, { weight = it })
        Spacer(Modifier.height(20.dp))
        Button(
            enabled = name.isNotBlank() && (height.toDoubleOrNull() ?: 0.0) > 0 && (weight.isBlank() || (weight.toDoubleOrNull() ?: 0.0) > 0),
            onClick = { onFinish(name.trim(), height.toDouble(), weight.toDoubleOrNull() ?: 0.0) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Start tracking") }
    }
}
