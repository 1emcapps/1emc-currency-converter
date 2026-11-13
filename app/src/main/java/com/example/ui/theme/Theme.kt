package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode(val label: String) {
  DARK("Dark"),
  LIGHT("Light"),
  SYSTEM("System"),
}

private val DarkFintechColorScheme = darkColorScheme(
  primary = BitcoinOrange,
  onPrimary = NavyVoid,
  primaryContainer = BitcoinOrangeSubtle,
  onPrimaryContainer = BitcoinOrangeBright,
  secondary = PositiveEmerald,
  onSecondary = NavyVoid,
  secondaryContainer = PositiveEmeraldSubtle,
  onSecondaryContainer = PositiveEmerald,
  tertiary = WarningAmber,
  onTertiary = NavyVoid,
  tertiaryContainer = WarningAmberSubtle,
  onTertiaryContainer = WarningAmber,
  background = NavyBackground,
  onBackground = TextLightPrimary,
  surface = NavySurface,
  onSurface = TextLightPrimary,
  surfaceVariant = NavySurfaceElevated,
  onSurfaceVariant = TextLightSecondary,
  surfaceContainerLow = NavySurface,
  surfaceContainer = NavySurfaceElevated,
  surfaceContainerHigh = NavySurfaceHighest,
  outline = NavyBorder,
  outlineVariant = Color(0xFF1D2A44),
  error = NegativeCoral,
  onError = Color.White,
  errorContainer = NegativeCoralSubtle,
  onErrorContainer = NegativeCoral,
)

private val LightFintechColorScheme = lightColorScheme(
  primary = BitcoinOrangeDeep,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFFEDD5),
  onPrimaryContainer = Color(0xFF7C2D12),
  secondary = Color(0xFF16A34A),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFDCFCE7),
  onSecondaryContainer = Color(0xFF14532D),
  tertiary = Color(0xFFD97706),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFEF3C7),
  onTertiaryContainer = Color(0xFF78350F),
  background = LightBackground,
  onBackground = TextDarkPrimary,
  surface = LightSurface,
  onSurface = TextDarkPrimary,
  surfaceVariant = LightSurfaceElevated,
  onSurfaceVariant = TextDarkSecondary,
  surfaceContainerLow = LightSurface,
  surfaceContainer = LightSurfaceElevated,
  surfaceContainerHigh = LightSurfaceHighest,
  outline = LightBorder,
  outlineVariant = Color(0xFFE2E8F0),
  error = Color(0xFFDC2626),
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF7F1D1D),
)

val AppShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CurrencyConverterTheme(
  themeMode: ThemeMode = ThemeMode.DARK,
  content: @Composable () -> Unit,
) {
  val useDark = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  val colorScheme = if (useDark) DarkFintechColorScheme else LightFintechColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
