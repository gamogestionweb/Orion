package com.orion.proyectoorion.vivo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.orion.proyectoorion.ui.*
import kotlinx.coroutines.delay

// ==========================================================
// PANTALLA: MODO VIVO (OpenAI Realtime API)
// ==========================================================

@Composable
fun ScreenVivoMode(
    apiKey: String,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var hasAudioPermission by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Iniciando...") }
    var transcriptText by remember { mutableStateOf("") }

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition(label = "vivo")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            statusText = "Conectando con OpenAI..."
            // Aquí iría la conexión real
        }
    }

    // Check permission on launch
    LaunchedEffect(Unit) {
        hasAudioPermission = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            statusText = "Conectando..."
            delay(1500)
            isConnected = true
            statusText = "Listo para escuchar"
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OrionBlack,
                        Color(0xFF0D1B2A),
                        OrionBlack
                    )
                )
            )
    ) {
        // Efecto de fondo animado
        if (isListening || isSpeaking) {
            Box(
                Modifier
                    .fillMaxSize()
                    .blur(100.dp)
            ) {
                Box(
                    Modifier
                        .size(300.dp)
                        .align(Alignment.Center)
                        .scale(pulseScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    if (isListening) OrionVivo.copy(alpha = glowAlpha * 0.3f)
                                    else OrionPurple.copy(alpha = glowAlpha * 0.3f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(OrionDarkGrey.copy(0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = OrionText)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(
                                if (isConnected) OrionGreen else OrionOrange,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isConnected) "CONECTADO" else "DESCONECTADO",
                        color = if (isConnected) OrionGreen else OrionOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Settings placeholder
                IconButton(
                    onClick = { /* TODO: Settings */ },
                    modifier = Modifier
                        .size(48.dp)
                        .background(OrionDarkGrey.copy(0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, null, tint = OrionText.copy(0.5f))
                }
            }

            Spacer(Modifier.weight(0.3f))

            // Logo y título
            Text(
                "ORION",
                color = OrionVivo,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp
            )
            Text(
                "VIVO",
                color = OrionText.copy(0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 6.sp
            )

            Spacer(Modifier.height(40.dp))

            // Orbe central animado
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(if (isListening || isSpeaking) pulseScale else 1f),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow
                Box(
                    Modifier
                        .size(200.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    when {
                                        isListening -> OrionVivo.copy(alpha = 0.2f)
                                        isSpeaking -> OrionPurple.copy(alpha = 0.2f)
                                        else -> OrionBlue.copy(alpha = 0.1f)
                                    },
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // Inner circle
                Box(
                    Modifier
                        .size(140.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    OrionDarkGrey,
                                    OrionMediumGrey
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Core
                    Box(
                        Modifier
                            .size(100.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        when {
                                            isListening -> OrionVivo
                                            isSpeaking -> OrionPurple
                                            isConnected -> OrionBlue
                                            else -> Color.Gray
                                        },
                                        when {
                                            isListening -> OrionVivo.copy(alpha = 0.3f)
                                            isSpeaking -> OrionPurple.copy(alpha = 0.3f)
                                            isConnected -> OrionBlue.copy(alpha = 0.3f)
                                            else -> Color.Gray.copy(alpha = 0.3f)
                                        }
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                isListening -> Icons.Default.Mic
                                isSpeaking -> Icons.Default.VolumeUp
                                else -> Icons.Default.RemoveRedEye
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Status text
            Text(
                statusText,
                color = OrionText,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            // Transcript area
            if (transcriptText.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 150.dp),
                    colors = CardDefaults.cardColors(containerColor = OrionDarkGrey.copy(0.7f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        transcriptText,
                        modifier = Modifier.padding(16.dp),
                        color = OrionText.copy(0.8f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.weight(0.5f))

            // Control buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute button
                IconButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .size(56.dp)
                        .background(OrionDarkGrey, CircleShape)
                ) {
                    Icon(Icons.Default.MicOff, null, tint = OrionText.copy(0.5f))
                }

                // Main action button
                Button(
                    onClick = {
                        if (isConnected) {
                            isListening = !isListening
                            statusText = if (isListening) "Escuchando..." else "Listo para escuchar"
                        }
                    },
                    modifier = Modifier
                        .size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) OrionRed else OrionVivo
                    ),
                    enabled = isConnected
                ) {
                    Icon(
                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        null,
                        tint = if (isListening) Color.White else OrionBlack,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Camera button (placeholder)
                IconButton(
                    onClick = { /* TODO: Camera */ },
                    modifier = Modifier
                        .size(56.dp)
                        .background(OrionDarkGrey, CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = OrionText.copy(0.5f))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Hint text
            Text(
                "Toca el botón para hablar",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
