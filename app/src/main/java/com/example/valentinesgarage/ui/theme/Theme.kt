package com.example.valentinesgarage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GarageOrangeLight,
    onPrimary = GarageNavy,
    secondary = GarageBlue,
    tertiary = GarageSuccess,
    background = GarageDarkBackground,
    surface = GarageDarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = GarageOrange,
    onPrimary = Color.White,
    secondary = GarageNavy,
    tertiary = GarageSuccess,
    background = GarageBackground,
    surface = GarageSurface,
    surfaceVariant = Color(0xFFE8EEF5),
    onSurfaceVariant = GarageSlate,
    error = GarageError
)

@Composable
fun ValentinesGarageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
