package com.orion.proyectoorion.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.orion.proyectoorion.R

// ==========================================================
// ORION TYPOGRAPHY SYSTEM
// ==========================================================

// Main display font - Bold and futuristic
val OrionDisplayFont = FontFamily(
    Font(R.font.orion_display_bold, FontWeight.Bold),
    Font(R.font.orion_display_black, FontWeight.Black),
    Font(R.font.orion_display_medium, FontWeight.Medium)
)

// Body text font - Clean and readable
val OrionBodyFont = FontFamily(
    Font(R.font.orion_body_regular, FontWeight.Normal),
    Font(R.font.orion_body_medium, FontWeight.Medium),
    Font(R.font.orion_body_semibold, FontWeight.SemiBold),
    Font(R.font.orion_body_bold, FontWeight.Bold)
)

// Monospace font for technical information
val OrionMonoFont = FontFamily(
    Font(R.font.orion_mono_regular, FontWeight.Normal),
    Font(R.font.orion_mono_medium, FontWeight.Medium)
)
