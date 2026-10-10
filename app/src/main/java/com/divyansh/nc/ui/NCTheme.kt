package com.divyansh.nc.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NCBlack = Color(0xFF100D18)
val NCSurface = Color(0xFF21182F)
val NCSurfaceHigher = Color(0xFF30243F)
val NCPurple = Color(0xFFBB86FC)
val NCGreen = Color(0xFF67E8A5)
val NCOrange = Color(0xFFFFB86B)
val NCRed = Color(0xFFFF6B81)
val NCYellow = Color(0xFFFFD66B)
val NCText = Color(0xFFF7F2FF)
val NCMuted = Color(0xFFB7A6D2)

private val palette = darkColorScheme(
    primary = NCPurple, onPrimary = NCBlack,
    secondary = NCGreen, onSecondary = NCBlack,
    tertiary = NCOrange, background = NCBlack, onBackground = NCText,
    surface = NCSurface, onSurface = NCText,
    surfaceVariant = NCSurfaceHigher, onSurfaceVariant = NCMuted,
    error = NCRed, onError = NCBlack
)

@Composable
fun NCTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = palette, content = content)
}
