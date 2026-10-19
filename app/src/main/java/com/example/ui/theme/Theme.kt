package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.core.model.ThemeMode

private val NilaDarkColorScheme = darkColorScheme(
    primary = NilaCyan,
    onPrimary = Color(0xFF032830),
    primaryContainer = Color(0xFF0C3D4D),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = NilaViolet,
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = NilaPurple,
    onTertiary = Color(0xFF3B0764),
    tertiaryContainer = Color(0xFF581C87),
    onTertiaryContainer = Color(0xFFF3E8FF),
    background = NilaSpaceDark,
    onBackground = NilaTextPrimaryDark,
    surface = NilaSurfaceDark,
    onSurface = NilaTextPrimaryDark,
    surfaceVariant = NilaSurfaceVariantDark,
    onSurfaceVariant = NilaTextSecondaryDark,
    outline = NilaBorderDark,
    error = NilaRose,
    onError = Color.White
)

private val NilaAmoledColorScheme = darkColorScheme(
    primary = NilaCyan,
    onPrimary = Color(0xFF032830),
    primaryContainer = Color(0xFF06222D),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = NilaViolet,
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF1F1D4F),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = NilaPurple,
    onTertiary = Color(0xFF3B0764),
    tertiaryContainer = Color(0xFF3B125C),
    onTertiaryContainer = Color(0xFFF3E8FF),
    background = NilaAmoledBlack,
    onBackground = Color(0xFFF8FAFC),
    surface = NilaAmoledSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF12121A),
    onSurfaceVariant = NilaTextSecondaryDark,
    outline = NilaAmoledBorder,
    error = NilaRose,
    onError = Color.White
)

private val NilaLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAE6FD),
    onPrimaryContainer = Color(0xFF034A6E),
    secondary = Color(0xFF6366F1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = Color(0xFF9333EA),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF581C87),
    background = NilaSpaceLight,
    onBackground = NilaTextPrimaryLight,
    surface = NilaSurfaceLight,
    onSurface = NilaTextPrimaryLight,
    surfaceVariant = NilaSurfaceVariantLight,
    onSurfaceVariant = NilaTextSecondaryLight,
    outline = NilaBorderLight,
    error = Color(0xFFE11D48),
    onError = Color.White
)

@Composable
fun NilaAiTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (themeMode == ThemeMode.LIGHT) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
        }
        themeMode == ThemeMode.AMOLED -> NilaAmoledColorScheme
        themeMode == ThemeMode.LIGHT -> NilaLightColorScheme
        else -> NilaDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards-compatible alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NilaAiTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        useDynamicColor = dynamicColor,
        content = content
    )
}
