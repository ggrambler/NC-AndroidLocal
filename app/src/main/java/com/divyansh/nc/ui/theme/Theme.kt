package com.divyansh.nc.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val NCDarkColorScheme = darkColorScheme(
    primary = NCPurple,
    onPrimary = NCBackground,
    secondary = NCGreen,
    onSecondary = NCBackground,
    tertiary = NCOrange,

    background = NCBackground,
    onBackground = NCTextPrimary,

    surface = NCSurface,
    onSurface = NCTextPrimary,

    surfaceVariant = NCSurfaceVariant,
    onSurfaceVariant = NCTextSecondary,

    error = NCRed,
    onError = NCBackground
)

@Composable
fun NCTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NCDarkColorScheme,
        typography = Typography,
        content = content
    )
}