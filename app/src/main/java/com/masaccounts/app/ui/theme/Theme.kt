package com.masaccounts.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = GoldLight,
    onPrimary = NavyDark,
    primaryContainer = NavyPrimary,
    onPrimaryContainer = GoldContainer,
    secondary = GoldPrimary,
    onSecondary = NavyDark,
    secondaryContainer = NavySecondary,
    onSecondaryContainer = Color.White,
    background = NavyDark,
    onBackground = Slate100,
    surface = Color(0xFF132238),
    onSurface = Slate100,
    surfaceVariant = Color(0xFF1E334D),
    onSurfaceVariant = Slate300,
    outline = Color(0xFF334E68),
    error = Color(0xFFF87171),
    onError = Color.Black
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyDark,
    onPrimaryContainer = GoldLight,
    secondary = GoldDark,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = NavyDark,
    tertiary = AccountingGreen,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate300,
    error = AccountingRed,
    onError = Color.White
  )

@Composable
fun MasAccountsTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MasAccountsTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
