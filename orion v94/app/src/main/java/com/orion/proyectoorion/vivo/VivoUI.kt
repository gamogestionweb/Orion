package com.orion.proyectoorion.vivo

import android.content.Context
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

private val Black = Color(0xFF000000)
private val DarkGrey = Color(0xFF0A0A0A)
private val Cyan = Color(0xFF00F5D4)
private val Green = Color(0xFF00E676)
private val Red = Color(0xFFFF6B6B)
private val Yellow = Color(0xFFFFD93D)
private val White = Color(0xFFFFFFFF)
private val Grey = Color(0xFF444444)
private val LightGrey = Color(0xFF888888)

@Composable
fun ScreenVivoMode(
    apiKey: String,
    vivoStrings: VivoStrings,
    userMemory: UserMemory? = null,
    defaultModel: OrionVivoEngine.VivoModel = OrionVivoEngine.VivoModel.REALTIME_MINI,
    onBack: () -> Unit,
    onMemoryScreen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(OrionVivoEngine.VivoState.DORMANT) }
    var error by remember { mutableStateOf<String?>(null) }
    var memory by remember { mutableStateOf(userMemory) }
    var selectedModel by remember { mutableStateOf(defaultModel) }
    var showModelSelector by remember { mutableStateOf(false) }

    var engine by remember { mutableStateOf<OrionVivoEngine?>(null) }

    // File picker para cargar memoria JSON (fallback si no hay onMemoryScreen)
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val json = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()
                
                if (json.isNotBlank()) {
                    val parsed = parseMemoryFromJson(json)
                    if (parsed != null) {
                        memory = parsed
                        engine?.loadMemoryFromJson(json)
                        // Guardar para futuras sesiones
                        context.getSharedPreferences("orion_vivo", Context.MODE_PRIVATE)
                            .edit()
                            .putString("memory_json", json)
                            .apply()
                    } else {
                        error = vivoStrings.invalidJsonFormat
                        scope.launch { delay(3000); error = null }
                    }
                }
            } catch (e: Exception) {
                error = "${vivoStrings.errorReadingFile}: ${e.message}"
                scope.launch { delay(3000); error = null }
            }
        }
    }

    // Cargar memoria guardada al inicio
    LaunchedEffect(Unit) {
        if (memory == null) {
            context.getSharedPreferences("orion_vivo", Context.MODE_PRIVATE)
                .getString("memory_json", null)?.let { json ->
                    parseMemoryFromJson(json)?.let { memory = it }
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            engine = OrionVivoEngine(
                context = context,
                apiKey = apiKey,
                model = selectedModel,
                callback = object : OrionVivoCallback {
                    override fun onStateChanged(s: OrionVivoEngine.VivoState) { state = s }
                    override fun onError(e: String) {
                        error = e
                        scope.launch { delay(3000); error = null }
                    }
                    override fun onMemoryLoaded(m: UserMemory?) { 
                        if (m != null) memory = m 
                    }
                }
            ).also {
                it.initialize()
                userMemory?.let { m -> it.setUserMemory(m) }
                it.start()
                isRunning = true
            }
        } else {
            Toast.makeText(context, vivoStrings.microphoneNeeded, Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose { engine?.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        // Fondo animado
        AnimatedBackground(state, isRunning)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Header(
                state = state,
                userName = memory?.userName,
                model = selectedModel,
                isRunning = isRunning,
                hasMemory = memory != null,
                onBack = { engine?.release(); onBack() },
                onModelClick = { if (!isRunning) showModelSelector = true },
                onMemoryClick = { 
                    if (!isRunning) {
                        // Si hay callback de pantalla de memoria, usarlo
                        if (onMemoryScreen != null) {
                            engine?.release()
                            onMemoryScreen()
                        } else {
                            // Fallback: abrir selector de archivo
                            filePickerLauncher.launch("application/json")
                        }
                    }
                }
            )

            // Info de memoria debajo del header si está cargada
            memory?.let { m ->
                if (!isRunning) {
                    Text(
                        "${m.userName} · ${m.facts.size} ${vivoStrings.memoryDataCount}",
                        color = Green.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Orbe central
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Orb(
                    state = state,
                    isRunning = isRunning,
                    vivoStrings = vivoStrings,
                    onTap = {
                        if (!isRunning) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onLongPress = {
                        if (isRunning) {
                            engine?.release()
                            engine = null
                            isRunning = false
                            state = OrionVivoEngine.VivoState.DORMANT
                        }
                    }
                )
            }

            // Indicador de estado
            if (isRunning) {
                StateIndicator(state, vivoStrings)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }

        // Selector de modelo
        if (showModelSelector) {
            ModelSelector(
                currentModel = selectedModel,
                vivoStrings = vivoStrings,
                onSelect = { 
                    selectedModel = it
                    showModelSelector = false 
                },
                onDismiss = { showModelSelector = false }
            )
        }

        // Error
        error?.let {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp, start = 20.dp, end = 20.dp),
                color = Red.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(it, modifier = Modifier.padding(14.dp), color = White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AnimatedBackground(state: OrionVivoEngine.VivoState, isRunning: Boolean) {
    val transition = rememberInfiniteTransition(label = "bg")
    
    val speed = when (state) {
        OrionVivoEngine.VivoState.SPEAKING -> 600
        OrionVivoEngine.VivoState.LISTENING -> 1200
        OrionVivoEngine.VivoState.THINKING -> 1800
        else -> 5000
    }
    
    val waveCount = when (state) {
        OrionVivoEngine.VivoState.SPEAKING -> 10
        OrionVivoEngine.VivoState.LISTENING -> 6
        OrionVivoEngine.VivoState.THINKING -> 4
        else -> 2
    }
    
    val intensity = when (state) {
        OrionVivoEngine.VivoState.SPEAKING -> 0.15f
        OrionVivoEngine.VivoState.LISTENING -> 0.08f
        OrionVivoEngine.VivoState.THINKING -> 0.05f
        else -> 0.02f
    }
    
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(speed, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    val color = when (state) {
        OrionVivoEngine.VivoState.LISTENING -> Green
        OrionVivoEngine.VivoState.SPEAKING -> Red
        OrionVivoEngine.VivoState.THINKING -> Yellow
        else -> Cyan
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.maxDimension * 0.9f

        if (!isRunning) {
            drawCircle(Cyan.copy(alpha = 0.015f), maxRadius * 0.25f, center)
            return@Canvas
        }

        repeat(waveCount) { i ->
            val progress = (phase + i.toFloat() / waveCount) % 1f
            val radius = progress * maxRadius
            val alpha = (1f - progress) * intensity
            val strokeWidth = if (state == OrionVivoEngine.VivoState.SPEAKING) 
                3f + sin(progress * 12f) * 2f else 1.5f
            
            drawCircle(color.copy(alpha = alpha), radius, center, style = Stroke(strokeWidth))
        }
        
        // Ondas extra cuando habla
        if (state == OrionVivoEngine.VivoState.SPEAKING) {
            repeat(4) { i ->
                val offset = (phase * 2 + i * 0.25f) % 1f
                val r = maxRadius * 0.15f + offset * maxRadius * 0.4f
                drawCircle(color.copy(alpha = (1f - offset) * 0.12f), r, center, style = Stroke(3f))
            }
        }
    }
}

@Composable
private fun Header(
    state: OrionVivoEngine.VivoState,
    userName: String?,
    model: OrionVivoEngine.VivoModel,
    isRunning: Boolean,
    hasMemory: Boolean,
    onBack: () -> Unit,
    onModelClick: () -> Unit,
    onMemoryClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp).background(DarkGrey, CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, "Volver", tint = White, modifier = Modifier.size(22.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ORION",
                color = Cyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp
            )
            
            // Chip del modelo (clickeable si no está corriendo)
            Surface(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isRunning) { onModelClick() },
                color = if (!isRunning) DarkGrey else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (model == OrionVivoEngine.VivoModel.REALTIME) "⚡ Pro" else "💰 Mini",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = if (!isRunning) Cyan else Grey,
                    fontSize = 10.sp
                )
            }
        }

        // Botón de memoria (cerebro)
        IconButton(
            onClick = onMemoryClick,
            enabled = !isRunning,
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (hasMemory) Green.copy(alpha = 0.2f) else DarkGrey,
                    CircleShape
                )
        ) {
            Text(
                "🧠",
                fontSize = 20.sp,
                modifier = Modifier.alpha(if (isRunning) 0.4f else 1f)
            )
        }
    }
}

// Parser de memoria JSON
private fun parseMemoryFromJson(json: String): UserMemory? {
    return try {
        val obj = org.json.JSONObject(json)
        
        val name = obj.optString("userName",
            obj.optString("name",
                obj.optString("user_name", "")))
        
        val facts = mutableListOf<String>()
        listOf("facts", "memories", "data").forEach { key ->
            obj.optJSONArray(key)?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optString(i)?.takeIf { it.isNotBlank() }?.let { facts.add(it) }
                }
            }
        }
        
        UserMemory(
            userName = name,
            facts = facts,
            preferences = obj.optString("preferences", ""),
            language = obj.optString("language", "es")
        )
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun ModelSelector(
    currentModel: OrionVivoEngine.VivoModel,
    vivoStrings: VivoStrings,
    onSelect: (OrionVivoEngine.VivoModel) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(32.dp)
                .clickable(enabled = false) {},
            color = DarkGrey,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    vivoStrings.modeTitle,
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OrionVivoEngine.VivoModel.values().forEach { model ->
                    val isSelected = model == currentModel
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(model) }
                            .then(
                                if (isSelected) Modifier.border(2.dp, Cyan, RoundedCornerShape(12.dp))
                                else Modifier
                            ),
                        color = if (isSelected) Cyan.copy(alpha = 0.15f) else Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (model == OrionVivoEngine.VivoModel.REALTIME) "⚡" else "💰",
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        model.displayName,
                                        color = if (isSelected) Cyan else White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        model.priceInfo,
                                        color = LightGrey,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                when (model) {
                                    OrionVivoEngine.VivoModel.REALTIME -> vivoStrings.modelRealtimeDesc
                                    OrionVivoEngine.VivoModel.REALTIME_MINI -> vivoStrings.modelRealtimeMiniDesc
                                },
                                color = Grey,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss) {
                    Text(vivoStrings.cancel, color = LightGrey)
                }
            }
        }
    }
}

@Composable
private fun Orb(
    state: OrionVivoEngine.VivoState,
    isRunning: Boolean,
    vivoStrings: VivoStrings,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "orb")

    val targetScale = when (state) {
        OrionVivoEngine.VivoState.SPEAKING -> 1.25f
        OrionVivoEngine.VivoState.LISTENING -> 1.12f
        OrionVivoEngine.VivoState.THINKING -> 1.06f
        else -> 1f
    }
    
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 180f),
        label = "scale"
    )

    val rotationSpeed = when (state) {
        OrionVivoEngine.VivoState.SPEAKING -> 2000
        OrionVivoEngine.VivoState.LISTENING -> 6000
        OrionVivoEngine.VivoState.THINKING -> 4000
        else -> 25000
    }
    
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(rotationSpeed, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    val pulseSpeed = when (state) {
        OrionVivoEngine.VivoState.SPEAKING -> 80
        OrionVivoEngine.VivoState.LISTENING -> 250
        OrionVivoEngine.VivoState.THINKING -> 400
        else -> 2500
    }
    
    val pulse by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(pulseSpeed), RepeatMode.Reverse),
        label = "pulse"
    )

    val color = when (state) {
        OrionVivoEngine.VivoState.LISTENING -> Green
        OrionVivoEngine.VivoState.SPEAKING -> Red
        OrionVivoEngine.VivoState.THINKING -> Yellow
        OrionVivoEngine.VivoState.READY -> Cyan
        OrionVivoEngine.VivoState.DORMANT -> Grey
    }

    Box(
        modifier = Modifier
            .size(220.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
            },
        contentAlignment = Alignment.Center
    ) {
        // Anillos
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringCount = when (state) {
                OrionVivoEngine.VivoState.SPEAKING -> 8
                OrionVivoEngine.VivoState.LISTENING -> 5
                else -> 3
            }
            
            repeat(ringCount) { i ->
                val startAngle = rotation + i * (360f / ringCount)
                val sweepAngle = when (state) {
                    OrionVivoEngine.VivoState.SPEAKING -> 30f + pulse * 25f
                    OrionVivoEngine.VivoState.LISTENING -> 45f + pulse * 15f
                    else -> 55f
                }
                val alpha = when (state) {
                    OrionVivoEngine.VivoState.SPEAKING -> 0.5f * pulse
                    OrionVivoEngine.VivoState.LISTENING -> 0.35f * pulse
                    else -> 0.12f
                }
                
                drawArc(
                    color.copy(alpha = alpha), startAngle, sweepAngle, false,
                    style = Stroke(if (state == OrionVivoEngine.VivoState.SPEAKING) 5f else 2.5f)
                )
            }
            
            if (state == OrionVivoEngine.VivoState.SPEAKING) {
                repeat(4) { i ->
                    val r = size.minDimension / 2 * (0.55f + i * 0.12f)
                    drawCircle(color.copy(alpha = 0.08f * pulse), r, style = Stroke(2f))
                }
            }
        }

        // Glow
        val glowAlpha = when (state) {
            OrionVivoEngine.VivoState.SPEAKING -> 0.5f * pulse
            OrionVivoEngine.VivoState.LISTENING -> 0.3f * pulse
            else -> 0.08f
        }
        
        Box(
            modifier = Modifier
                .size(175.dp)
                .background(
                    Brush.radialGradient(listOf(color.copy(alpha = glowAlpha), Color.Transparent)),
                    CircleShape
                )
        )

        // Centro
        val centerAlpha = when (state) {
            OrionVivoEngine.VivoState.SPEAKING -> 0.75f + pulse * 0.25f
            OrionVivoEngine.VivoState.LISTENING -> 0.55f + pulse * 0.2f
            else -> 0.35f
        }
        
        Box(
            modifier = Modifier
                .size(130.dp)
                .background(
                    Brush.radialGradient(listOf(
                        color.copy(alpha = centerAlpha),
                        color.copy(alpha = centerAlpha * 0.25f),
                        color.copy(alpha = 0.03f)
                    )),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (state) {
                    OrionVivoEngine.VivoState.DORMANT -> "💤"
                    OrionVivoEngine.VivoState.LISTENING -> "👂"
                    OrionVivoEngine.VivoState.THINKING -> "💭"
                    OrionVivoEngine.VivoState.SPEAKING -> "🗣"
                    OrionVivoEngine.VivoState.READY -> "✨"
                },
                fontSize = 52.sp,
                modifier = Modifier.alpha(if (state == OrionVivoEngine.VivoState.SPEAKING) pulse else 1f)
            )
        }

        if (!isRunning) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(vivoStrings.tapToWake, color = Grey, fontSize = 10.sp)
                Text(vivoStrings.holdToStop, color = Grey.copy(alpha = 0.5f), fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun StateIndicator(state: OrionVivoEngine.VivoState, vivoStrings: VivoStrings) {
    val color = when (state) {
        OrionVivoEngine.VivoState.LISTENING -> Green
        OrionVivoEngine.VivoState.SPEAKING -> Red
        OrionVivoEngine.VivoState.THINKING -> Yellow
        else -> Cyan
    }
    
    val text = when (state) {
        OrionVivoEngine.VivoState.LISTENING -> vivoStrings.statesListening.removeSuffix("...")
        OrionVivoEngine.VivoState.SPEAKING -> vivoStrings.statesSpeaking.removeSuffix("...")
        OrionVivoEngine.VivoState.THINKING -> vivoStrings.statesThinking.removeSuffix("...")
        else -> vivoStrings.statesReady
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
