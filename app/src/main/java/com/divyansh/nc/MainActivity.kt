
package com.divyansh.nc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.divyansh.nc.navigation.NCTab
import com.divyansh.nc.ui.components.NCBottomBar
import com.divyansh.nc.ui.theme.NCTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NCTheme {
                NCApp()
            }
        }
    }
}

@Composable
fun NCApp() {

    var selectedTabName by rememberSaveable {
        mutableStateOf(NCTab.NUTRITION.name)
    }

    val selectedTab = NCTab.valueOf(selectedTabName)

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "NC Settings",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleLarge
                )

                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Export Reports") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
            }
        }
    ) {

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,

            topBar = {
                NCHeader(
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            },

            bottomBar = {
                NCBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTabName = tab.name
                    }
                )
            }

        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),

                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                when (selectedTab) {

                    NCTab.REFERENCES -> {
                        PlaceholderScreen(
                            title = "References",
                            description = "Your meal plan CSV references will appear here."
                        )
                    }

                    NCTab.NUTRITION -> {
                        PlaceholderScreen(
                            title = "Nutrition",
                            description = "Track calories and protein here."
                        )
                    }

                    NCTab.TRAINING -> {
                        PlaceholderScreen(
                            title = "Training",
                            description = "Your daily workout will appear here."
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NCHeader(
    onMenuClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column {
                Text(
                    text = "NC - Nutrition & Conditioning",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    text = "Hello Divyansh!",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            IconButton(
                onClick = onMenuClick
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Open menu"
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    description: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )

    Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge
    )

    repeat(12) { index ->
        Text(
            text = "Placeholder item ${index + 1}",
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
