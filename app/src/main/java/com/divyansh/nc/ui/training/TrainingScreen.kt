
package com.divyansh.nc.ui.training

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrainingScreen() {
    Column {
        Text(
            text = "Training Tracker",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Your daily workout will appear here.",
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Exercise history and progression coming soon.",
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
