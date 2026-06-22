package com.ytsave.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val TealPrimaryDark = Color(0xFF00BCD4)
val TealOnPrimaryDark = Color(0xFF003544)
val TealPrimaryContainerDark = Color(0xFF004D61)
val TealOnPrimaryContainerDark = Color(0xFFA6EEFF)

val AmberSecondaryDark = Color(0xFFFFB74D)
val AmberOnSecondaryDark = Color(0xFF462A00)

val SurfaceDark = Color(0xFF121212)
val BackgroundDark = Color(0xFF0A0A0A)
val SurfaceVariantDark = Color(0xFF1E1E1E)
val TextPrimaryDark = Color(0xFFE3E3E3)

val TealPrimaryLight = Color(0xFF006879)
val TealOnPrimaryLight = Color(0xFFFFFFFF)
val TealPrimaryContainerLight = Color(0xFFA6EEFF)

val AmberSecondaryLight = Color(0xFFFF8F00)

val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealOnPrimaryContainerDark,
    secondary = AmberSecondaryDark,
    onSecondary = AmberOnSecondaryDark,
    surface = SurfaceDark,
    background = BackgroundDark,
    onSurface = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    error = Color(0xFFCF6679)
)

val LightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealOnPrimaryLight,
    primaryContainer = TealPrimaryContainerLight,
    secondary = AmberSecondaryLight,
    surface = Color(0xFFFAFAFA),
    background = Color(0xFFFFFFFF)
)
