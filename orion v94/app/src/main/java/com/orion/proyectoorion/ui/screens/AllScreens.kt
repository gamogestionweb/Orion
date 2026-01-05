package com.orion.proyectoorion.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.proyectoorion.R
import com.orion.proyectoorion.data.*
import com.orion.proyectoorion.models.*
import com.orion.proyectoorion.ui.*
import com.orion.proyectoorion.viewmodel.BrainViewModel
import com.orion.proyectoorion.viewmodel.ProcessingState
import org.json.JSONArray
import org.json.JSONObject

// ==========================================================
// PANTALLA: SELECCIÓN DE IDIOMA
// ==========================================================

@Composable
fun ScreenLanguage(onLanguageSelected: (String) -> Unit) {
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
            modifier = Modifier.size(100.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("ORION", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
        Text("AI ASSISTANT", color = Color.Gray, fontSize = 12.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(40.dp))

        ALL_LANGUAGES.forEach { (name, code, flag) ->
            OutlinedButton(
                onClick = { onLanguageSelected(code) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF333333)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = OrionDarkGrey.copy(0.5f))
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(flag, fontSize = 24.sp)
                    Spacer(Modifier.width(16.dp))
                    Text(name, fontSize = 16.sp, color = OrionText)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = OrionBlue.copy(0.6f))
                }
            }
        }
    }
}

// ==========================================================
// PANTALLA: SELECCIÓN DE MOTOR
// ==========================================================

@Composable
fun ScreenEngineSelect(
    viewModel: BrainViewModel,
    onCloudSelected: () -> Unit,
    onLocalSelected: () -> Unit,
    onVivoSelected: () -> Unit,
    onEmergencySelected: () -> Unit
) {
    val ui = viewModel.uiStrings
    val ctx = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "vivo")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(OrionBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(ui.engineSelectTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(32.dp))

        // MODO NUBE
        Card(
            onClick = onCloudSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = CardDefaults.cardColors(containerColor = OrionDarkGrey),
            border = BorderStroke(1.dp, OrionPurple.copy(0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .background(OrionPurple.copy(0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("☁", fontSize = 28.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(ui.engineCloudTitle, color = OrionPurple, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(ui.engineCloudDesc, color = Color.Gray, fontSize = 13.sp)
                    Text("🇺🇸 Gemini · ChatGPT · Claude", color = OrionBlue.copy(0.7f), fontSize = 11.sp)
                    Text("🇨🇳 DeepSeek · Qwen · Kimi", color = OrionOrange.copy(0.7f), fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // MODO LOCAL
        Card(
            onClick = onLocalSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = CardDefaults.cardColors(containerColor = OrionDarkGrey),
            border = BorderStroke(1.dp, OrionGreen.copy(0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .background(OrionGreen.copy(0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📱", fontSize = 28.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(ui.engineLocalTitle, color = OrionGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(ui.engineLocalDesc, color = Color.Gray, fontSize = 13.sp)
                    Text(ui.noAutoMemory, color = OrionOrange.copy(0.8f), fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // MODO VIVO
        Card(
            onClick = onVivoSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            colors = CardDefaults.cardColors(containerColor = OrionDarkGrey),
            border = BorderStroke(
                2.dp,
                Brush.linearGradient(
                    colors = listOf(
                        OrionVivo.copy(alpha = glowAlpha),
                        OrionVivo.copy(alpha = glowAlpha * 0.3f),
                        OrionVivo.copy(alpha = glowAlpha)
                    )
                )
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    OrionVivo.copy(alpha = 0.3f * glowAlpha),
                                    OrionVivo.copy(alpha = 0.1f)
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👁", fontSize = 28.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        ui.engineVivoTitle,
                        color = OrionVivo,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(ui.engineVivoDesc, color = Color.Gray, fontSize = 13.sp)
                }
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    null,
                    tint = OrionVivo.copy(alpha = glowAlpha)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // MODO EMERGENCIA
        Card(
            onClick = onEmergencySelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            colors = CardDefaults.cardColors(containerColor = OrionDarkGrey),
            border = BorderStroke(2.dp, OrionRed.copy(0.6f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .background(OrionRed.copy(0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🆘", fontSize = 28.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(ui.engineEmergencyTitle, color = OrionRed, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(ui.engineEmergencyDesc, color = Color.Gray, fontSize = 13.sp)
                }
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    null,
                    tint = OrionRed
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ==========================================================
// PANTALLA: PROVEEDORES CLOUD
// ==========================================================

@Composable
fun ScreenCloudProviders(
    viewModel: BrainViewModel,
    onBack: () -> Unit,
    onProviderSelected: (CloudProvider) -> Unit,
    onSelectModels: (CloudProvider) -> Unit
) {
    val ui = viewModel.uiStrings
    val ctx = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(OrionBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text(ui.cloudProviderTitle, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(ui.cloudProviderDesc, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        Text(ui.allProvidersMemory, color = OrionGreen.copy(0.8f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(20.dp))

        CloudProvider.entries.forEach { p ->
            val hasKey = viewModel.hasApiKeyFor(p)
            val iconResId = ctx.resources.getIdentifier(p.iconResName, "drawable", ctx.packageName)
            val selectedModel = viewModel.getSelectedCloudModel(p)
            var showModelDropdown by remember { mutableStateOf(false) }

            Card(
                onClick = { onProviderSelected(p) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = OrionDarkGrey),
                border = BorderStroke(1.dp, if (hasKey) p.color.copy(0.6f) else Color.Gray.copy(0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(50.dp)
                                .background(p.color.copy(0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconResId != 0) {
                                Image(
                                    painter = painterResource(id = iconResId),
                                    contentDescription = p.displayName,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Text(
                                    when (p) {
                                        CloudProvider.GEMINI -> "✨"
                                        CloudProvider.OPENAI -> "🤖"
                                        CloudProvider.CLAUDE -> "🧠"
                                        CloudProvider.DEEPSEEK -> "🐋"
                                        CloudProvider.QWEN -> "🔮"
                                        CloudProvider.KIMI -> "🌙"
                                    },
                                    fontSize = 24.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.displayName, color = p.color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(p.country, fontSize = 14.sp)
                            }
                            Text(p.getDescription(ui.langCode), color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Gray.copy(0.5f))
                    }

                    Spacer(Modifier.height(12.dp))

                    // Selector de modelo
                    Box {
                        OutlinedButton(
                            onClick = { showModelDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, p.color.copy(0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = OrionMediumGrey)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(selectedModel.displayName, color = OrionText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${ui.inputPrice}\$${selectedModel.inputPrice} · ${ui.outputPrice}\$${selectedModel.outputPrice}${ui.perMillion}",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = p.color.copy(0.7f), modifier = Modifier.size(20.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showModelDropdown,
                            onDismissRequest = { showModelDropdown = false },
                            modifier = Modifier.background(OrionDarkGrey)
                        ) {
                            p.models.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    model.displayName,
                                                    color = if (model.recommended) p.color else OrionText,
                                                    fontWeight = if (model.recommended) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (model.recommended) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(color = p.color.copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                                                        Text("★", color = p.color, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }
                                            Text(model.getDescription(ui.langCode), color = Color.Gray, fontSize = 11.sp)
                                            Text(
                                                "${ui.inputPrice}\$${model.inputPrice} · ${ui.outputPrice}\$${model.outputPrice} · ${ui.contextWindow}${model.contextWindow}K",
                                                color = OrionBlue.copy(0.7f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectCloudModel(p, model)
                                        showModelDropdown = false
                                    },
                                    modifier = Modifier.background(
                                        if (selectedModel.id == model.id) p.color.copy(0.1f) else Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (hasKey) ui.apiConfigured else ui.apiNotConfigured,
                            color = if (hasKey) OrionGreen else OrionOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.keyUrl)))
                        }) {
                            Text(ui.getApiKey, color = OrionBlue, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ==========================================================
// PANTALLA: MODELOS DE UN PROVEEDOR
// ==========================================================

@Composable
fun ScreenProviderModels(
    viewModel: BrainViewModel,
    provider: CloudProvider,
    onBack: () -> Unit,
    onModelSelected: (CloudModel) -> Unit
) {
    val ui = viewModel.uiStrings
    val selectedModel = viewModel.getSelectedCloudModel(provider)

    Column(
        Modifier
            .fillMaxSize()
            .background(OrionBlack)
            .padding(24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = OrionText)
            }
            Spacer(Modifier.width(8.dp))
            Text(provider.displayName, color = provider.color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(provider.models) { model ->
                Card(
                    onClick = { onModelSelected(model) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedModel.id == model.id) provider.color.copy(0.1f) else OrionDarkGrey
                    ),
                    border = BorderStroke(
                        1.dp, 
                        if (selectedModel.id == model.id) provider.color else Color.Gray.copy(0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(model.displayName, color = OrionText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (model.recommended) {
                                Spacer(Modifier.width(8.dp))
                                Surface(color = provider.color.copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                                    Text("★ Recomendado", color = provider.color, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(model.getDescription(ui.langCode), color = Color.Gray, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Text("${ui.inputPrice} \$${model.inputPrice}", color = OrionGreen, fontSize = 12.sp)
                            Spacer(Modifier.width(12.dp))
                            Text("${ui.outputPrice} \$${model.outputPrice}", color = OrionOrange, fontSize = 12.sp)
                            Spacer(Modifier.width(12.dp))
                            Text("${ui.contextWindow} ${model.contextWindow}K", color = OrionBlue, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// PANTALLA: CONFIGURAR API KEY
// ==========================================================

@Composable
fun ScreenSetupApi(
    viewModel: BrainViewModel,
    provider: CloudProvider,
    onBack: () -> Unit,
    onApiConfigured: () -> Unit
) {
    val ui = viewModel.uiStrings
    val ctx = LocalContext.current
    var key by remember { mutableStateOf("") }
    val iconResId = ctx.resources.getIdentifier(provider.iconResName, "drawable", ctx.packageName)
    val selectedModel = viewModel.getSelectedCloudModel(provider)

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(50.dp))
        Box(
            Modifier
                .size(60.dp)
                .background(provider.color.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (iconResId != 0) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = provider.displayName,
                    modifier = Modifier.size(38.dp)
                )
            } else {
                Text(
                    when (provider) {
                        CloudProvider.GEMINI -> "✨"
                        CloudProvider.OPENAI -> "🤖"
                        CloudProvider.CLAUDE -> "🧠"
                        CloudProvider.DEEPSEEK -> "🐋"
                        CloudProvider.QWEN -> "🔮"
                        CloudProvider.KIMI -> "🌙"
                    },
                    fontSize = 28.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(provider.displayName, color = provider.color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(provider.country, fontSize = 16.sp)
        }
        Text(selectedModel.displayName, color = Color.Gray, fontSize = 14.sp)
        Text(
            "${ui.inputPrice}\$${selectedModel.inputPrice} · ${ui.outputPrice}\$${selectedModel.outputPrice}${ui.perMillion}",
            color = OrionBlue.copy(0.7f),
            fontSize = 12.sp
        )
        Text(
            ui.getApiKey,
            color = OrionBlue,
            fontSize = 13.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(provider.keyUrl))) }
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text(ui.setupCloudLabel) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = provider.color,
                unfocusedBorderColor = Color.Gray.copy(0.3f),
                cursorColor = provider.color
            ),
            singleLine = true
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { 
                viewModel.setupApiKey(key) 
                onApiConfigured()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = provider.color),
            shape = RoundedCornerShape(12.dp),
            enabled = key.length > 10
        ) {
            Text(ui.setupCloudBtn, color = OrionBlack, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ==========================================================
// PANTALLA: MODELOS LOCALES
// ==========================================================

@Composable
fun ScreenLocalModels(
    viewModel: BrainViewModel,
    onBack: () -> Unit,
    onModelReady: () -> Unit
) {
    val ui = viewModel.uiStrings
    val ctx = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(OrionBlack)
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Text(ui.modelSelectTitle, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(ui.modelSelectDesc, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = OrionOrange.copy(0.1f)),
            border = BorderStroke(1.dp, OrionOrange.copy(0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(ui.noMemoryWarning, color = OrionOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (viewModel.isLoadingModel) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OrionDarkGrey),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OrionGreen, strokeWidth = 2.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(ui.loadingModel, color = OrionText)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(AVAILABLE_MODELS) { model ->
                LocalModelCard(
                    model = model,
                    isDownloaded = viewModel.isModelDownloaded(model),
                    isDownloading = viewModel.downloadingModelId == model.id,
                    downloadProgress = if (viewModel.downloadingModelId == model.id) viewModel.downloadProgress else 0f,
                    isSelected = viewModel.selectedModel?.id == model.id,
                    onDownload = { viewModel.downloadModel(ctx, model) },
                    onUse = { 
                        viewModel.selectAndLoadModel(ctx, model)
                        onModelReady()
                    },
                    onDelete = { viewModel.deleteModel(model) },
                    ui = ui
                )
            }
        }
    }
}

@Composable
fun LocalModelCard(
    model: AvailableModel,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    isSelected: Boolean,
    onDownload: () -> Unit,
    onUse: () -> Unit,
    onDelete: () -> Unit,
    ui: UiStrings
) {
    val bc = when {
        isSelected -> OrionGreen
        model.recommended -> OrionOrange.copy(0.5f)
        else -> Color.Gray.copy(0.2f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = OrionDarkGrey),
        border = BorderStroke(1.dp, bc),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(model.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (model.recommended) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = OrionOrange.copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("★", color = OrionOrange, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(model.getDescription(ui.langCode), color = Color.Gray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text("${ui.modelSize} ${model.sizeGB} GB", color = OrionBlue.copy(0.7f), fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isDownloading) {
                Column {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = OrionGreen,
                        trackColor = OrionMediumGrey
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${ui.downloadProgress}${(downloadProgress * 100).toInt()}%", color = OrionGreen, fontSize = 12.sp)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDownloaded) {
                        Button(
                            onClick = onUse,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) OrionGreen else OrionGreen.copy(0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                ui.modelUse,
                                color = if (isSelected) OrionBlack else OrionGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.height(40.dp),
                            border = BorderStroke(1.dp, OrionRed.copy(0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = OrionRed, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrionBlue.copy(0.15f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⬇", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(ui.modelDownload, color = OrionBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// PANTALLA: CHAT
// ==========================================================

@Composable
fun ScreenChat(
    viewModel: BrainViewModel,
    showMemory: Boolean,
    onToggleMemory: () -> Unit,
    onChangeEngine: () -> Unit,
    onLogout: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(OrionBlack)) {
        Column(Modifier.fillMaxSize()) {
            ChatTopBar(viewModel, onToggleMemory, onChangeEngine)
            Box(Modifier.weight(1f)) {
                if (showMemory) {
                    MemoryContent(viewModel, onLogout)
                } else {
                    ChatList(viewModel)
                }
            }
            if (!showMemory) ChatInputBar(viewModel)
        }
    }
}

@Composable
fun ChatTopBar(
    viewModel: BrainViewModel,
    onToggleMemory: () -> Unit,
    onChangeEngine: () -> Unit
) {
    val ui = viewModel.uiStrings
    val ac = when {
        viewModel.isLocalMode -> OrionGreen
        else -> viewModel.activeCloudProvider?.color ?: OrionPurple
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(OrionDarkGrey.copy(0.95f), OrionBlack.copy(0f))))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .clickable { onChangeEngine() }
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ORION", color = ac, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 3.sp)
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = ui.changeProvider,
                    tint = Color.Gray.copy(0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val modelName = when {
                    viewModel.isLocalMode -> viewModel.selectedModel?.name?.take(18) ?: "LOCAL"
                    else -> {
                        val provider = viewModel.activeCloudProvider
                        if (provider != null) viewModel.getSelectedCloudModel(provider).displayName else "CLOUD"
                    }
                }
                Text(modelName, color = Color.Gray, fontSize = 10.sp, letterSpacing = 1.sp)
                viewModel.activeCloudProvider?.let { p ->
                    if (!viewModel.isLocalMode) {
                        Spacer(Modifier.width(4.dp))
                        Text(p.country, fontSize = 10.sp)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = { viewModel.startNewChat() },
                modifier = Modifier
                    .size(44.dp)
                    .background(OrionDarkGrey, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = ui.newChat, tint = ac, modifier = Modifier.size(22.dp))
            }
            IconButton(
                onClick = onToggleMemory,
                modifier = Modifier
                    .size(44.dp)
                    .background(OrionDarkGrey, CircleShape)
            ) {
                Text("🧠", fontSize = 22.sp)
            }
        }
    }
}

@Composable
fun ChatList(viewModel: BrainViewModel) {
    val listState = rememberLazyListState()
    val ac = when {
        viewModel.isLocalMode -> OrionGreen
        else -> viewModel.activeCloudProvider?.color ?: OrionPurple
    }

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp)
    ) {
        items(viewModel.messages.reversed()) { (msg, isUser) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                Surface(
                    color = if (isUser) ac else OrionDarkGrey,
                    shape = if (isUser)
                        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                    else
                        RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Text(
                        msg,
                        Modifier.padding(14.dp),
                        color = if (isUser) Color.Black else OrionText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        if (viewModel.isLoading && (viewModel.messages.isEmpty() || viewModel.messages.last().second)) {
            item { ThinkingIndicator(viewModel) }
        }
    }
}

@Composable
fun ThinkingIndicator(viewModel: BrainViewModel) {
    val ui = viewModel.uiStrings
    val inf = rememberInfiniteTransition(label = "thinking")
    val d1 by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "d1")
    val d2 by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing, delayMillis = 200), RepeatMode.Reverse), label = "d2")
    val d3 by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing, delayMillis = 400), RepeatMode.Reverse), label = "d3")

    val txt = when (viewModel.processingState) {
        ProcessingState.THINKING -> ui.thinking
        ProcessingState.ANALYZING -> ui.analyzing
        ProcessingState.RESPONDING -> ui.responding
        ProcessingState.IDLE -> ""
    }

    val col = when (viewModel.processingState) {
        ProcessingState.THINKING -> OrionYellow
        ProcessingState.ANALYZING -> OrionBlue
        ProcessingState.RESPONDING -> if (viewModel.isLocalMode) OrionGreen else viewModel.activeCloudProvider?.color ?: OrionPurple
        ProcessingState.IDLE -> Color.Gray
    }

    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(32.dp)
                .background(col.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (viewModel.processingState) {
                    ProcessingState.THINKING -> "🤔"
                    ProcessingState.ANALYZING -> "🔍"
                    ProcessingState.RESPONDING -> "💭"
                    ProcessingState.IDLE -> "⏳"
                },
                fontSize = 16.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(txt, color = col, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(4.dp))
        Text("●", color = col.copy(alpha = d1), fontSize = 8.sp)
        Spacer(Modifier.width(2.dp))
        Text("●", color = col.copy(alpha = d2), fontSize = 8.sp)
        Spacer(Modifier.width(2.dp))
        Text("●", color = col.copy(alpha = d3), fontSize = 8.sp)
    }
}

@Composable
fun ChatInputBar(viewModel: BrainViewModel) {
    val ui = viewModel.uiStrings
    var text by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val ac = when {
        viewModel.isLocalMode -> OrionGreen
        else -> viewModel.activeCloudProvider?.color ?: OrionPurple
    }

    Surface(
        modifier = Modifier
            .imePadding()
            .navigationBarsPadding(),
        color = OrionBlack,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = Color.DarkGray.copy(0.3f), thickness = 0.5.dp)
            Row(
                Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(OrionDarkGrey)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(ui.chatPlaceholder, color = Color.Gray, fontSize = 16.sp)
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    ) { it() }
                }

                Spacer(Modifier.width(10.dp))

                IconButton(
                    onClick = {
                        if (text.isNotBlank() && !viewModel.isLoading) {
                            viewModel.sendMessage(text.trim())
                            text = ""
                        } else if (viewModel.isLoading) {
                            Toast.makeText(ctx, ui.waitPrevious, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (viewModel.isLoading) Color.Gray.copy(0.3f) else ac, CircleShape),
                    enabled = text.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        null,
                        tint = if (viewModel.isLoading) Color.Gray else OrionBlack,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ==========================================================
// CONTENIDO DE MEMORIA (dentro del chat)
// ==========================================================

@Composable
fun MemoryContent(viewModel: BrainViewModel, onLogout: () -> Unit) {
    val ctx = LocalContext.current
    val ui = viewModel.uiStrings
    var showDeleteAll by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Int?>(null) }
    var itemToEdit by remember { mutableStateOf<Int?>(null) }
    var editText by remember { mutableStateOf("") }
    var newMemoryText by remember { mutableStateOf("") }
    val ac = when {
        viewModel.isLocalMode -> OrionGreen
        else -> viewModel.activeCloudProvider?.color ?: OrionPurple
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let { u ->
            ctx.contentResolver.openOutputStream(u)?.use { s ->
                s.write(viewModel.memoryJson.toByteArray())
            }
            Toast.makeText(ctx, ui.toastBackup, Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { u ->
            ctx.contentResolver.openInputStream(u)?.bufferedReader()?.use { r ->
                r.readText()
            }?.let { j ->
                viewModel.importarDatos(j)
                Toast.makeText(ctx, ui.toastImport, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text(ui.alertFormatTitle, color = OrionText, fontWeight = FontWeight.Bold) },
            text = { Text(ui.alertFormatText, color = Color.LightGray) },
            containerColor = OrionDarkGrey,
            shape = RoundedCornerShape(16.dp),
            confirmButton = {
                TextButton(onClick = {
                    viewModel.borrarDatos()
                    showDeleteAll = false
                    Toast.makeText(ctx, ui.toastFormat, Toast.LENGTH_SHORT).show()
                }) {
                    Text(ui.btnConfirm, color = OrionRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll = false }) {
                    Text(ui.btnCancel, color = Color.Gray)
                }
            }
        )
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(ui.alertDeleteOneTitle, color = OrionText, fontWeight = FontWeight.Bold) },
            text = { Text(ui.alertDeleteOneText, color = Color.LightGray) },
            containerColor = OrionDarkGrey,
            shape = RoundedCornerShape(16.dp),
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarMemoria(itemToDelete!!)
                    itemToDelete = null
                }) {
                    Text(ui.btnDelete, color = OrionRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(ui.btnCancel, color = Color.Gray)
                }
            }
        )
    }

    // Diálogo para editar memoria
    if (itemToEdit != null) {
        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text(ui.memEditTitle, color = OrionText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(ui.memEditHint, color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OrionText,
                            unfocusedTextColor = OrionText,
                            focusedBorderColor = ac,
                            unfocusedBorderColor = Color.Gray.copy(0.5f),
                            cursorColor = ac
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }
            },
            containerColor = OrionDarkGrey,
            shape = RoundedCornerShape(16.dp),
            confirmButton = {
                TextButton(onClick = {
                    viewModel.editarMemoria(itemToEdit!!, editText)
                    itemToEdit = null
                    editText = ""
                    Toast.makeText(ctx, ui.toastMemoryUpdated, Toast.LENGTH_SHORT).show()
                }) {
                    Text(ui.btnSave, color = ac, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null; editText = "" }) {
                    Text(ui.btnCancel, color = Color.Gray)
                }
            }
        )
    }

    val memorias = remember(viewModel.memoryJson) {
        try {
            val r = JSONObject(viewModel.memoryJson)
            val arr = r.optJSONArray("memorias") ?: JSONArray()
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val mem = arr.optString(i, "")
                if (mem.isNotBlank()) list.add(mem)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(OrionBlack)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(ui.memTitle, color = ac, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(ui.memSubtitle, color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))

        // Aviso modo local
        if (viewModel.isLocalMode) {
            Card(
                colors = CardDefaults.cardColors(containerColor = OrionOrange.copy(0.1f)),
                border = BorderStroke(1.dp, OrionOrange.copy(0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📖", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(ui.memoryWarningLocal, color = OrionOrange, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ========================================
        // AÑADIR NUEVA MEMORIA MANUALMENTE
        // ========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = ac.copy(0.08f)),
            border = BorderStroke(1.dp, ac.copy(0.3f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("➕", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(ui.memAddTitle, color = ac, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMemoryText,
                        onValueChange = { newMemoryText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(ui.memAddPlaceholder, color = Color.Gray.copy(0.6f), fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OrionText,
                            unfocusedTextColor = OrionText,
                            focusedBorderColor = ac,
                            unfocusedBorderColor = Color.Gray.copy(0.3f),
                            cursorColor = ac,
                            focusedContainerColor = OrionDarkGrey,
                            unfocusedContainerColor = OrionDarkGrey
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Spacer(Modifier.width(10.dp))
                    IconButton(
                        onClick = {
                            if (newMemoryText.isNotBlank()) {
                                viewModel.agregarMemoriaManual(newMemoryText.trim())
                                newMemoryText = ""
                                Toast.makeText(ctx, ui.toastMemoryAdded, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(ac, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, null, tint = OrionBlack, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // Sección de memorias
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(ac.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("💭", fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(ui.memoriesSection, color = ac, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            // Contador de memorias
            Text(
                "${memorias.size} ${if (memorias.size == 1) ui.memCountSingular else ui.memCountPlural}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(12.dp))

        if (memorias.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = OrionDarkGrey),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🧠", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        ui.memEmpty,
                        color = Color.DarkGray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = OrionDarkGrey),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    memorias.forEachIndexed { i, memoria ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    editText = memoria
                                    itemToEdit = i 
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .background(ac.copy(0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💡", fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                memoria,
                                color = OrionText,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                                lineHeight = 20.sp
                            )
                            // Botón editar
                            IconButton(
                                onClick = { 
                                    editText = memoria
                                    itemToEdit = i 
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    null,
                                    tint = ac.copy(0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            // Botón eliminar
                            IconButton(
                                onClick = { itemToDelete = i },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = OrionRed.copy(0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (i < memorias.size - 1) {
                            HorizontalDivider(
                                color = Color.Gray.copy(0.2f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 44.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Sección de acciones
        Text(ui.actionsSection, color = ac.copy(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                colors = ButtonDefaults.buttonColors(containerColor = OrionDarkGrey),
                modifier = Modifier.weight(1f).height(70.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📂", fontSize = 24.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(ui.memBtnImport, color = OrionBlue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                }
            }
            Button(
                onClick = { exportLauncher.launch("Orion_Memoria_Backup.json") },
                colors = ButtonDefaults.buttonColors(containerColor = OrionDarkGrey),
                modifier = Modifier.weight(1f).height(70.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💾", fontSize = 24.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(ui.memBtnExport, color = OrionGreen, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { showDeleteAll = true },
            colors = ButtonDefaults.buttonColors(containerColor = OrionDarkGrey),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🗑️", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(ui.memBtnFormat, color = OrionRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            border = BorderStroke(1.dp, OrionRed.copy(0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🚪", fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Text(ui.memBtnLogout, color = OrionRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(20.dp))
    }
}

// ==========================================================
// PANTALLA: MEMORIA (standalone)
// ==========================================================

@Composable
fun ScreenMemory(
    viewModel: BrainViewModel,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(OrionBlack)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = OrionText)
            }
            Spacer(Modifier.width(8.dp))
            Text(viewModel.uiStrings.memTitle, color = OrionText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        MemoryContent(viewModel, onLogout = {})
    }
}
