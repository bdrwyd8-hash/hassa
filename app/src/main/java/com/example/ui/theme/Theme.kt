package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val TrailKitDarkColorScheme = darkColorScheme(
    primary = ForestGreenBright,
    onPrimary = Color(0xFF0A140F),
    primaryContainer = ForestGreenContainer,
    onPrimaryContainer = TextPrimaryDark,
    secondary = AmberHighlight,
    onSecondary = Color(0xFF1F1605),
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberSoft,
    tertiary = ForestGreenPrimary,
    onTertiary = Color.White,
    background = CharcoalBg,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = CharcoalBorder,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedContainer,
    onErrorContainer = Color(0xFFFFDAD6)
)

private val TrailKitLightColorScheme = lightColorScheme(
    primary = ForestGreenLightPrimary,
    onPrimary = Color.White,
    primaryContainer = ForestGreenLightContainer,
    onPrimaryContainer = TextPrimaryLight,
    secondary = AmberLightHighlight,
    onSecondary = Color.White,
    secondaryContainer = AmberLightContainer,
    onSecondaryContainer = Color(0xFF3B2600),
    tertiary = ForestGreenPrimary,
    onTertiary = Color.White,
    background = AlpineBg,
    onBackground = TextPrimaryLight,
    surface = AlpineSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = AlpineCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = AlpineBorder,
    error = DangerRed,
    onError = Color.White
)

val TrailKitShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun TrailKitTheme(
    darkTheme: Boolean = true,
    isRtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TrailKitDarkColorScheme else TrailKitLightColorScheme
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = TrailKitShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    TrailKitTheme(darkTheme = darkTheme, isRtl = false, content = content)
}
