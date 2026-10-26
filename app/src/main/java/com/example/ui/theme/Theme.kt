package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BmkComfortColorScheme = lightColorScheme(
    primary = BmkNavy,
    onPrimary = Color.White,
    primaryContainer = BmkOrangeContainer,
    onPrimaryContainer = BmkOrangeDark,
    secondary = BmkOrange,
    onSecondary = Color.White,
    secondaryContainer = BmkBlueBg,
    onSecondaryContainer = BmkBlue,
    tertiary = BmkGreen,
    onTertiary = Color.White,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorderColor
)

@Composable
fun BmkExpressTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BmkNavy.toArgb()
            window.navigationBarColor = Color.White.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = BmkComfortColorScheme,
        typography = Typography,
        content = content
    )
}
