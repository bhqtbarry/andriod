package cn.syphotos.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Cloud,
    primaryContainer = BlueSoft,
    onPrimaryContainer = Night,
    secondary = Cyan,
    onSecondary = Cloud,
    secondaryContainer = Mist,
    onSecondaryContainer = Night,
    tertiary = Amber,
    onTertiary = Night,
    tertiaryContainer = Mint,
    onTertiaryContainer = Night,
    background = Cloud,
    onBackground = Night,
    surface = Cloud,
    onSurface = Night,
    surfaceVariant = BlueSoft,
    onSurfaceVariant = Steel,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F4FB),
    outline = Color(0xFFD1D9E6),
    scrim = Night.copy(alpha = 0.72f),
)

@Composable
fun SyPhotosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}
