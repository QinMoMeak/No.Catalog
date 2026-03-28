package com.nocatalog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = InkBlue,
    secondary = Clay,
    tertiary = Moss,
    background = Sand,
    surface = androidx.compose.ui.graphics.Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Mist,
    secondary = Clay,
    tertiary = Moss,
)

/**
 * 应用主题，先建立稳定配色，后续再细化视觉层级。
 */
@Composable
fun NoCatalogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NoCatalogTypography,
        content = content,
    )
}

