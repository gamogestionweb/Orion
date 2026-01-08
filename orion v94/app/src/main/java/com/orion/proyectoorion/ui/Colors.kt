package com.orion.proyectoorion.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================================
// ORION COLOR SYSTEM - Professional & Polished
// ==========================================================

// ===== Background Colors =====
val OrionBlack = Color(0xFF0A0A0A)
val OrionDarkGrey = Color(0xFF161616)
val OrionMediumGrey = Color(0xFF222222)
val OrionLightGrey = Color(0xFF2A2A2A)
val OrionSurfaceVariant = Color(0xFF1E1E1E)

// ===== Primary Accent Colors =====
val OrionPurple = Color(0xFFBB86FC)
val OrionPurpleLight = Color(0xFFD0A3FF)
val OrionPurpleDark = Color(0xFF9965E8)

val OrionBlue = Color(0xFF3DDCFF)
val OrionBlueLight = Color(0xFF6FE9FF)
val OrionBlueDark = Color(0xFF00B8D4)

val OrionGreen = Color(0xFF69F0AE)
val OrionGreenLight = Color(0xFF98FFC7)
val OrionGreenDark = Color(0xFF3DCF8E)

val OrionVivo = Color(0xFF00F5D4)
val OrionVivoLight = Color(0xFF33FFF0)
val OrionVivoDark = Color(0xFF00C9B0)

// ===== Semantic Colors =====
val OrionRed = Color(0xFFCF6679)
val OrionRedLight = Color(0xFFFF8A95)
val OrionRedDark = Color(0xFFB23850)

val OrionOrange = Color(0xFFFFAB40)
val OrionOrangeLight = Color(0xFFFFD180)
val OrionOrangeDark = Color(0xFFFF8F00)

val OrionYellow = Color(0xFFFFD54F)
val OrionYellowLight = Color(0xFFFFE57F)
val OrionYellowDark = Color(0xFFFFC107)

// ===== Text Colors =====
val OrionText = Color(0xFFE8E8E8)
val OrionTextSecondary = Color(0xFFB0B0B0)
val OrionTextTertiary = Color(0xFF808080)
val OrionTextDisabled = Color(0xFF505050)

// ===== Gradient Brushes =====
val OrionGradientPurple = Brush.horizontalGradient(
    colors = listOf(OrionPurpleDark, OrionPurple, OrionPurpleLight)
)

val OrionGradientBlue = Brush.horizontalGradient(
    colors = listOf(OrionBlueDark, OrionBlue, OrionBlueLight)
)

val OrionGradientGreen = Brush.horizontalGradient(
    colors = listOf(OrionGreenDark, OrionGreen, OrionGreenLight)
)

val OrionGradientVivo = Brush.horizontalGradient(
    colors = listOf(OrionVivoDark, OrionVivo, OrionVivoLight)
)

val OrionGradientRed = Brush.horizontalGradient(
    colors = listOf(OrionRedDark, OrionRed, OrionRedLight)
)

val OrionGradientOrange = Brush.horizontalGradient(
    colors = listOf(OrionOrangeDark, OrionOrange, OrionOrangeLight)
)

// Radial gradients for special effects
val OrionRadialPurple = Brush.radialGradient(
    colors = listOf(OrionPurpleLight.copy(alpha = 0.3f), Color.Transparent)
)

val OrionRadialVivo = Brush.radialGradient(
    colors = listOf(OrionVivoLight.copy(alpha = 0.3f), Color.Transparent)
)
