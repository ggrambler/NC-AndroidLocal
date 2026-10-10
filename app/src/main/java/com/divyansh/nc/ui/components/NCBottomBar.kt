
package com.divyansh.nc.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.divyansh.nc.navigation.NCTab

@Composable
fun NCBottomBar(
    selectedTab: NCTab,
    onTabSelected: (NCTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NCTab.entries.forEach { tab ->

            val icon = when (tab) {
                NCTab.REFERENCES -> Icons.Filled.MenuBook
                NCTab.NUTRITION -> Icons.Filled.Restaurant
                NCTab.TRAINING -> Icons.Filled.FitnessCenter
            }

            NavigationBarItem(
                selected = selectedTab == tab,

                onClick = {
                    onTabSelected(tab)
                },

                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.title
                    )
                },

                label = {
                    Text(tab.title)
                }
            )
        }
    }
}
