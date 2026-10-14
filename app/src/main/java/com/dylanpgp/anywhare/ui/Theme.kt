package com.dylanpgp.anywhare.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.dylanpgp.anywhare.data.ThemeMode

private val Mint = Color(0xFF2DF5B0)

private val DarkScheme = darkColorScheme(
    primary = Mint, onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF005139), onPrimaryContainer = Color(0xFF8CFFD0),
    background = Color(0xFF101412), surface = Color(0xFF101412),
    surfaceContainer = Color(0xFF1C201E), surfaceContainerHigh = Color(0xFF262A28),
)

private val AmoledScheme = darkColorScheme(
    primary = Mint, onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF005139), onPrimaryContainer = Color(0xFF8CFFD0),
    background = Color.Black, surface = Color.Black,
    surfaceContainer = Color(0xFF0E0E0E), surfaceContainerHigh = Color(0xFF181818),
    surfaceVariant = Color(0xFF1A1A1A),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF006C4F), onPrimary = Color.White,
    primaryContainer = Color(0xFF8CFFD0), onPrimaryContainer = Color(0xFF002117),
    background = Color(0xFFF6FBF7), surface = Color(0xFFF6FBF7),
    surfaceContainer = Color(0xFFEAEFEB), surfaceContainerHigh = Color(0xFFE4E9E5),
)

private val PurpleScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC), tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF15101F), surface = Color(0xFF15101F),
    onBackground = Color(0xFFE8E0F0), onSurface = Color(0xFFE8E0F0),
    surfaceContainer = Color(0xFF211A2E), surfaceContainerHigh = Color(0xFF2B2338),
    surfaceVariant = Color(0xFF2B2338),
)

@Composable
fun AnyWhareTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val scheme = when (mode) {
        ThemeMode.SYSTEM -> when {
            Build.VERSION.SDK_INT >= 31 && systemDark -> dynamicDarkColorScheme(context)
            Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
            systemDark -> DarkScheme
            else -> LightScheme
        }
        ThemeMode.AMOLED -> AmoledScheme
        ThemeMode.LIGHT -> LightScheme
        ThemeMode.PURPLE -> PurpleScheme
    }
    val dark = scheme.background.luminance() < 0.5f

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
