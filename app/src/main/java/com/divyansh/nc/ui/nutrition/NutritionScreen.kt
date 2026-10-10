
package com.divyansh.nc.ui.nutrition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NutritionScreen() {
    Column {
        Text(
            text = "Nutrition Tracker",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Calories and protein tracking",
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Animated concentric progress rings coming next.",
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
