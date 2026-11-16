package io.github.pikatchu2k3.oneplusdebloat.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

private val Light = lightColorScheme(
    primary = Color(0xFF006B5E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9FF2E0),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4A635D),
    secondaryContainer = Color(0xFFCCE8E0),
    tertiary = Color(0xFF446179),
    tertiaryContainer = Color(0xFFCAE6FF),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF5FBF7),
    surface = Color(0xFFF5FBF7),
    surfaceVariant = Color(0xFFDAE5E0),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF83D5C5),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFF9FF2E0),
    secondary = Color(0xFFB1CCC4),
    secondaryContainer = Color(0xFF334B45),
    tertiary = Color(0xFFABCAE5),
    tertiaryContainer = Color(0xFF2C4960),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    background = Color(0xFF0E1513),
    surface = Color(0xFF0E1513),
    surfaceVariant = Color(0xFF3F4945),
)

/** Extra colours for the package states; Material has no "success" or "warning" role. */
data class StateColors(val ok: Color, val onOk: Color, val warn: Color, val onWarn: Color)

val LocalStateColors = staticCompositionLocalOf {
    StateColors(Color(0xFFBEEBC8), Color(0xFF0B3D1C), Color(0xFFFFE08A), Color(0xFF4A3800))
}

val LocalTxt = staticCompositionLocalOf<Txt> { En }

@Composable
fun CleanupTheme(
    dark: Boolean = isSystemInDarkTheme(),
    german: Boolean = Locale.getDefault().language == "de",
    dynamic: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> Dark
        else -> Light
    }
    val stateColors = if (dark) {
        StateColors(Color(0xFF1F5130), Color(0xFFBEEBC8), Color(0xFF5C4700), Color(0xFFFFE08A))
    } else {
        StateColors(Color(0xFFBEEBC8), Color(0xFF0B3D1C), Color(0xFFFFE08A), Color(0xFF4A3800))
    }
    CompositionLocalProvider(
        LocalTxt provides (if (german) De else En),
        LocalStateColors provides stateColors,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
