package com.orion.proyectoorion.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.proyectoorion.R
import com.orion.proyectoorion.data.ALL_LANGUAGES
import com.orion.proyectoorion.ui.*

// ==========================================================
// IMPROVED LANGUAGE SCREEN
// ==========================================================

@Composable
fun ImprovedScreenLanguage(onLanguageSelected: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(OrionBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_icon),
            contentDescription = "Orion",
            modifier = Modifier.size(110.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "ORION",
            color = OrionPurpleLight,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            fontFamily = OrionDisplayFont,
            letterSpacing = 10.sp
        )
        Text(
            "AI ASSISTANT",
            color = OrionTextSecondary,
            fontSize = 13.sp,
            fontFamily = OrionMonoFont,
            letterSpacing = 5.sp
        )
        Spacer(Modifier.height(50.dp))

        ALL_LANGUAGES.forEach { (name, code, flag) ->
            OrionSecondaryButton(
                text = "$flag  $name",
                onClick = { onLanguageSelected(code) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                accentColor = OrionPurple,
                icon = Icons.Default.KeyboardArrowRight
            )
        }
    }
}
