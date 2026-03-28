package com.nocatalog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = LavenderPrimary,
    onPrimary = Color.White,
    secondary = RoseSecondary,
    onSecondary = DeepPlum,
    tertiary = AccentGold,
    onTertiary = SlateText,
    background = BlushBackground,
    onBackground = SlateText,
    surface = WarmSurface,
    onSurface = SlateText,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = BlueGreyText,
    outline = OutlinePink,
    primaryContainer = LavenderSecondary,
    onPrimaryContainer = DeepPlum,
    secondaryContainer = RoseSecondary,
    onSecondaryContainer = SlateText,
)

private val DarkColors = darkColorScheme(
    primary = LavenderSecondary,
    onPrimary = DeepPlum,
    secondary = RoseSecondary,
    onSecondary = SlateText,
    tertiary = AccentGold,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF43384E),
    onSurfaceVariant = Color(0xFFD6CAD8),
)

private val NoCatalogShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
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
        shapes = NoCatalogShapes,
        content = content,
    )
}
