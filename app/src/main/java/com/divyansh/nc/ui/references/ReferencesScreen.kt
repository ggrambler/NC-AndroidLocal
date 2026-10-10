package com.divyansh.nc.ui.references

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import java.lang.ref.Reference

@Composable
fun ReferencesScreen(){
    Column() {
        Text(
            text = "Food References",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Your saved meal plans will appear here.",
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "CSV import and editable tables coming soon.",
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}