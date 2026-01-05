package com.orion.proyectoorion.emergency

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.orion.proyectoorion.ui.*
import java.text.SimpleDateFormat
import java.util.*

private val Green = Color(0xFF00E676)
private val Red = Color(0xFFFF1744)
private val Orange = Color(0xFFFF9100)

// ============ GENERADOR QR ============
fun generateQRBitmap(content: String, size: Int = 512): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenEmergencyMode(langCode: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val str = remember(langCode) { getEmergencyStrings(langCode) }
    val mesh = remember { MeshEmergency.getInstance(ctx) }
    val contacts = remember { EmergencyContacts(ctx) }

    val isActive by mesh.isActive.collectAsState()
    val peerCount by mesh.peerCount.collectAsState()
    val messages by mesh.messages.collectAsState()
    val status by mesh.status.collectAsState()

    var msgText by remember { mutableStateOf("") }
    var encrypted by remember { mutableStateOf(false) }
    var selContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var contactList by remember { mutableStateOf(contacts.getContacts()) }
    var showAddContact by remember { mutableStateOf(false) }
    var showMyCode by remember { mutableStateOf(false) }
    var showContacts by remember { mutableStateOf(false) }

    // Scanner QR
    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { code ->
            if (code.length > 50) {
                // Código válido escaneado, mostrar diálogo para añadir
                showAddContact = true
                // Guardamos el código escaneado en un estado temporal
            }
        }
    }
    var scannedCode by remember { mutableStateOf<String?>(null) }
    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { code ->
            if (code.length > 50) {
                scannedCode = code
                showAddContact = true
            } else {
                Toast.makeText(ctx, "❌ QR no válido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permisos
    val perms = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }
    var hasPerms by remember { mutableStateOf(perms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPerms = it.values.all { v -> v }
    }

    val wifi = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val loc = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var wifiOn by remember { mutableStateOf(wifi.isWifiEnabled) }
    var locOn by remember { mutableStateOf(loc.isProviderEnabled(LocationManager.GPS_PROVIDER)) }

    LaunchedEffect(Unit) {
        while (true) {
            wifiOn = wifi.isWifiEnabled
            locOn = loc.isProviderEnabled(LocationManager.GPS_PROVIDER)
            hasPerms = perms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
            kotlinx.coroutines.delay(2000)
        }
    }

    // Pedir permisos automáticamente al entrar
    LaunchedEffect(Unit) {
        if (!hasPerms) {
            permLauncher.launch(perms)
        }
    }

    DisposableEffect(Unit) { onDispose { mesh.deactivate() } }

    val ready = hasPerms && wifiOn && locOn

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(str.title, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        // Indicador más grande y con borde para mejor visibilidad
                        Box(
                            Modifier
                                .size(14.dp)
                                .border(1.dp, Color.White.copy(0.5f), CircleShape)
                                .padding(2.dp)
                                .background(
                                    when { peerCount > 0 -> Green; isActive -> Orange; else -> Color.Gray },
                                    CircleShape
                                )
                        )
                        if (isActive) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (peerCount > 0) "✓ ${peerCount} ${if (peerCount == 1) str.deviceSingular else str.devicePlural}" else status,
                                color = if (peerCount > 0) Green else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = if (peerCount > 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { mesh.deactivate(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    // MI QR (verde, destacado)
                    IconButton(onClick = { showMyCode = true }) {
                        Icon(Icons.Default.QrCode2, "Mi QR", tint = Green)
                    }
                    // MIS CONTACTOS
                    IconButton(onClick = { showContacts = true }) {
                        Badge(containerColor = if (contactList.isNotEmpty()) Green else Color.Transparent) {
                            Icon(Icons.Default.People, "Contactos", tint = Color.White)
                        }
                    }
                    // ESCANEAR para añadir
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val options = ScanOptions().apply {
                                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                setPrompt(str.scanPrompt)
                                setBeepEnabled(false)
                                setOrientationLocked(true)
                                captureActivity = PortraitCaptureActivity::class.java
                            }
                            qrScanLauncher.launch(options)
                        } else {
                            permLauncher.launch(perms)
                        }
                    }) {
                        Icon(Icons.Default.QrCodeScanner, "Escanear", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OrionBlack)
            )
        },
        containerColor = OrionBlack
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp)) {

            // ===== REQUISITOS (siempre visibles si faltan) =====
            if (!hasPerms || !wifiOn || !locOn) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!hasPerms) {
                        TextButton(onClick = { permLauncher.launch(perms) }) {
                            Text(str.permWarning, color = Orange, fontSize = 12.sp)
                        }
                    }
                    if (!wifiOn) {
                        TextButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }) {
                            Text(str.wifiWarning, color = Orange, fontSize = 12.sp)
                        }
                    }
                    if (!locOn) {
                        TextButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) {
                            Text(str.locationWarning, color = Orange, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ===== PANEL DE ACTIVACIÓN =====
            if (!isActive) {
                // Explicación breve cuando no está activo
                Card(
                    colors = CardDefaults.cardColors(containerColor = OrionDarkGrey),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            str.infoTitle,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            str.infoDesc,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = Green, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                str.infoEncrypted,
                                color = Green.copy(0.9f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, null, tint = Orange, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                str.infoPublic,
                                color = Orange.copy(0.9f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        
                        // === COMPARTIR APP ===
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color.Gray.copy(0.3f))
                        Spacer(Modifier.height(10.dp))
                        
                        val apkSize = remember { ApkSharing.getApkSize(ctx) }
                        OutlinedButton(
                            onClick = { ApkSharing.shareApk(ctx, langCode) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.Gray.copy(0.5f))
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when (langCode.lowercase()) {
                                    "es" -> "Compartir Orion ($apkSize)"
                                    "ca" -> "Compartir Orion ($apkSize)"
                                    "pt" -> "Compartilhar Orion ($apkSize)"
                                    "fr" -> "Partager Orion ($apkSize)"
                                    "de" -> "Orion teilen ($apkSize)"
                                    "zh" -> "分享 Orion ($apkSize)"
                                    else -> "Share Orion ($apkSize)"
                                },
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            when (langCode.lowercase()) {
                                "es" -> "Pásale la app a quien no la tenga"
                                "ca" -> "Passa l'app a qui no la tingui"
                                "pt" -> "Passe o app para quem não tem"
                                "fr" -> "Partagez l'app avec ceux qui ne l'ont pas"
                                "de" -> "Teile die App mit anderen"
                                "zh" -> "分享给没有此应用的人"
                                else -> "Share the app with those who don't have it"
                            },
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ===== BOTÓN ACTIVAR =====
            Button(
                onClick = { if (isActive) mesh.deactivate() else mesh.activate() },
                colors = ButtonDefaults.buttonColors(containerColor = if (isActive) Red else Green),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = ready || isActive
            ) {
                Icon(if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isActive) str.btnDeactivate else str.btnActivate,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (peerCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text("• $peerCount ${if (peerCount == 1) str.deviceSingular else str.devicePlural}", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ===== CONTACTOS RÁPIDOS (chips horizontales) =====
            if (contactList.isNotEmpty()) {
                Text(str.contactsLabel, color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(contactList) { contact ->
                        FilterChip(
                            selected = selContact == contact,
                            onClick = {
                                selContact = if (selContact == contact) null else contact
                                if (selContact != null) encrypted = true
                            },
                            label = { Text(contact.name, maxLines = 1) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier.size(20.dp).background(
                                        if (selContact == contact) Green else Color.Gray,
                                        CircleShape
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(contact.name.take(1).uppercase(), fontSize = 10.sp, color = Color.White)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Green.copy(0.2f),
                                selectedLabelColor = Green
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ===== ENVIAR MENSAJE =====
            if (isActive) {
                // Banner de estado de conexión claro
                if (peerCount == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Orange.copy(0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Wifi, null, tint = Orange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            str.messageSearching,
                            color = Orange,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Green.copy(0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            str.messageReady,
                            color = Green,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Card(colors = CardDefaults.cardColors(containerColor = OrionDarkGrey), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = msgText,
                                onValueChange = { if (it.length <= 500) msgText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(str.messagePlaceholder, color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = if (encrypted) Green else OrionPurple,
                                    unfocusedBorderColor = Color.Gray.copy(0.3f)
                                ),
                                maxLines = 2,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (msgText.isNotBlank()) {
                                        if (encrypted && selContact != null) {
                                            mesh.sendPrivate(msgText, selContact!!.id, selContact!!.publicKey)
                                        } else {
                                            mesh.sendPublic(msgText)
                                        }
                                        msgText = ""
                                    }
                                })
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (msgText.isNotBlank()) {
                                        if (encrypted && selContact != null) {
                                            mesh.sendPrivate(msgText, selContact!!.id, selContact!!.publicKey)
                                        } else {
                                            mesh.sendPublic(msgText)
                                        }
                                        msgText = ""
                                    }
                                },
                                enabled = msgText.isNotBlank() && (!encrypted || selContact != null)
                            ) {
                                Icon(
                                    Icons.Default.Send, null,
                                    tint = if (msgText.isNotBlank()) Green else Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Cifrado + Destinatario
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Row(
                                Modifier.clickable {
                                    encrypted = !encrypted
                                    if (!encrypted) selContact = null
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = encrypted,
                                    onCheckedChange = { encrypted = it; if (!it) selContact = null },
                                    colors = CheckboxDefaults.colors(checkedColor = Green, uncheckedColor = Color.Gray)
                                )
                                Icon(Icons.Default.Lock, null, tint = if (encrypted) Green else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(str.encryptedLabel, color = if (encrypted) Green else Color.Gray, fontSize = 12.sp)
                            }

                            if (encrypted) {
                                Spacer(Modifier.weight(1f))
                                if (selContact != null) {
                                    Surface(
                                        color = Green.copy(0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${str.recipientFor} ${selContact!!.name}", color = Green, fontSize = 12.sp)
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Close, null,
                                                tint = Green,
                                                modifier = Modifier.size(14.dp).clickable { selContact = null }
                                            )
                                        }
                                    }
                                } else {
                                    Text(str.selectRecipient, color = Orange, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ===== LISTA MENSAJES =====
            Text(str.messagesTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))

            if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Forum, null, tint = Color.Gray.copy(0.4f), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(if (isActive) str.messagesEmpty else str.messagesEmptyInactive, color = Color.Gray)

                        if (contactList.isEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { showMyCode = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Green)
                            ) {
                                Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(str.shareQrBtn)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(messages) { msg ->
                        MsgCard(msg, mesh.myId, contactList, str) { mesh.decrypt(it) }
                    }
                }
            }
        }
    }

    // ===== DIÁLOGO: MI CÓDIGO QR =====
    if (showMyCode) {
        val qrBitmap = remember { generateQRBitmap(mesh.myKey, 400) }

        AlertDialog(
            onDismissRequest = { showMyCode = false },
            containerColor = OrionDarkGrey,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode2, null, tint = Green)
                    Spacer(Modifier.width(8.dp))
                    Text(str.myQrTitle, color = Color.White)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        str.myQrDesc,
                        color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))

                    // QR Visual
                    qrBitmap?.let { bitmap ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR",
                                modifier = Modifier.size(200.dp).padding(8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("ID: ${mesh.myId}", color = Green, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                    Spacer(Modifier.height(8.dp))

                    // Código texto colapsable
                    var showText by remember { mutableStateOf(false) }
                    TextButton(onClick = { showText = !showText }) {
                        Text(if (showText) str.myQrHideCode else str.myQrShowCode, color = Color.Gray, fontSize = 11.sp)
                    }
                    AnimatedVisibility(visible = showText) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                            Text(
                                mesh.myKey,
                                color = Green.copy(0.8f),
                                fontSize = 6.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("key", mesh.myKey))
                        Toast.makeText(ctx, str.myQrCopied, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrionPurple)
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(4.dp))
                    Text(str.myQrCopy)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMyCode = false }) {
                    Text(str.btnClose, color = Color.Gray)
                }
            }
        )
    }

    // ===== DIÁLOGO: AÑADIR CONTACTO =====
    if (showAddContact) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf(scannedCode ?: "") }

        AlertDialog(
            onDismissRequest = { showAddContact = false; scannedCode = null },
            containerColor = OrionDarkGrey,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, null, tint = Green)
                    Spacer(Modifier.width(8.dp))
                    Text(str.addContactTitle, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(str.addContactName) },
                        placeholder = { Text(str.addContactNameHint) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray) }
                    )

                    if (scannedCode != null) {
                        // Código ya escaneado
                        Card(colors = CardDefaults.cardColors(containerColor = Green.copy(0.15f))) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Green)
                                Spacer(Modifier.width(8.dp))
                                Text(str.qrScanned, color = Green)
                            }
                        }
                    } else {
                        // Opción de escanear o pegar
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text(str.addContactCode) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 3,
                            trailingIcon = {
                                IconButton(onClick = {
                                    val options = ScanOptions().apply {
                                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        setPrompt(str.scanPrompt)
                                        setBeepEnabled(false)
                                        setOrientationLocked(true)
                                        captureActivity = PortraitCaptureActivity::class.java
                                    }
                                    qrScanLauncher.launch(options)
                                }) {
                                    Icon(Icons.Default.QrCodeScanner, str.scanQrBtn, tint = Green)
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalCode = scannedCode ?: code
                        if (contacts.addContact(name.trim(), finalCode) != null) {
                            contactList = contacts.getContacts()
                            Toast.makeText(ctx, "✅ ${name.trim()} ${str.contactAdded}", Toast.LENGTH_SHORT).show()
                            showAddContact = false
                            scannedCode = null
                        } else {
                            Toast.makeText(ctx, str.invalidCode, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = name.isNotBlank() && (code.isNotBlank() || scannedCode != null),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Icon(Icons.Default.PersonAdd, null)
                    Spacer(Modifier.width(4.dp))
                    Text(str.addContactBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContact = false; scannedCode = null }) {
                    Text(str.btnCancel, color = Color.Gray)
                }
            }
        )
    }

    // ===== DIÁLOGO: LISTA DE CONTACTOS =====
    if (showContacts) {
        AlertDialog(
            onDismissRequest = { showContacts = false },
            containerColor = OrionDarkGrey,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, null, tint = Green)
                    Spacer(Modifier.width(8.dp))
                    Text("${str.contactsTitle} (${contactList.size})", color = Color.White)
                }
            },
            text = {
                if (contactList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PersonOff, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(str.contactsEmpty, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text(str.contactsEmptyHint, color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(contactList) { contact ->
                            Card(colors = CardDefaults.cardColors(containerColor = OrionBlack)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar
                                    Box(
                                        modifier = Modifier.size(40.dp).background(Green.copy(0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            contact.name.take(1).uppercase(),
                                            color = Green,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    // Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, color = Color.White, fontWeight = FontWeight.Medium)
                                        Text(
                                            "ID: ${contact.id}",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // Eliminar
                                    IconButton(onClick = {
                                        contacts.removeContact(contact.id)
                                        contactList = contacts.getContacts()
                                        if (selContact?.id == contact.id) selContact = null
                                        Toast.makeText(ctx, str.contactDeleted, Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Delete, null, tint = Red.copy(0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showContacts = false  // Cerrar PRIMERO el diálogo
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt(str.scanPrompt)
                            setBeepEnabled(false)
                            setOrientationLocked(true)
                            captureActivity = PortraitCaptureActivity::class.java
                        }
                        qrScanLauncher.launch(options)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null)
                    Spacer(Modifier.width(4.dp))
                    Text(str.scanQrBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showContacts = false }) {
                    Text(str.btnClose, color = Color.Gray)
                }
            }
        )
    }
}

// ============ CARD DE MENSAJE (original) ============
@Composable
fun MsgCard(msg: MeshMessage, myId: String, contacts: List<EmergencyContact>, str: EmergencyStrings, decrypt: (MeshMessage) -> String) {
    val isMe = msg.from == myId
    val isForMe = msg.to == myId
    val isPublic = !msg.enc

    val sender = if (isMe) str.fromMe else contacts.find { it.id == msg.from }?.name ?: msg.fromName
    val recipient = msg.to?.let { if (it == myId) str.fromMe.lowercase() else contacts.find { c -> c.id == it }?.name ?: "?" }

    val content = when {
        isPublic -> msg.text
        isForMe || isMe -> decrypt(msg)
        else -> str.privateMsg
    }

    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.ts))

    Card(
        colors = CardDefaults.cardColors(containerColor = when {
            isMe -> OrionPurple.copy(0.25f)
            isForMe -> Green.copy(0.15f)
            else -> OrionDarkGrey
        }),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isMe && recipient != null) "${str.fromMe} → $recipient" else sender,
                        color = when { isMe -> OrionPurple; isForMe -> Green; else -> Color.White },
                        fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    if (msg.enc) Icon(Icons.Default.Lock, null, tint = Green, modifier = Modifier.size(14.dp))
                    else Text("📢", fontSize = 12.sp)
                }
                Text(time, color = Color.Gray, fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(content, color = Color.White, fontSize = 15.sp)
            if (msg.hops > 0) Text("↪ ${msg.hops} ${str.hopsLabel}", color = Color.Gray, fontSize = 10.sp)
        }
    }
}