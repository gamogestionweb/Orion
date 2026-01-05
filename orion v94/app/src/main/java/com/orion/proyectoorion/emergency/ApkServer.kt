package com.orion.proyectoorion.emergency

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ApkServer - Servidor HTTP para compartir Orion
 * 
 * Usa LocalOnlyHotspot (Android 8+) para crear un punto de acceso
 * al que otros dispositivos pueden conectarse SIN confirmación manual.
 * 
 * El receptor escanea el QR WiFi → se conecta automáticamente → descarga el APK
 * 
 * Velocidad: ~5-10 MB/s vs ~300 KB/s de Bluetooth
 */
@SuppressLint("MissingPermission")
class ApkServer(private val context: Context) {
    
    companion object {
        private const val TAG = "ApkServer"
        private const val HTTP_PORT = 8080
    }
    
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isRunning = AtomicBoolean(false)
    
    private var apkFile: File? = null
    
    // LocalOnlyHotspot
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private var hotspotSsid: String? = null
    private var hotspotPassword: String? = null
    
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()
    
    private val _downloadCount = MutableStateFlow(0)
    val downloadCount: StateFlow<Int> = _downloadCount.asStateFlow()
    
    /**
     * Inicia el servidor de APK
     * 1. Crea hotspot local
     * 2. Prepara el APK
     * 3. Levanta servidor HTTP
     */
    fun start(langCode: String = "es", onReady: (url: String) -> Unit, onError: (String) -> Unit) {
        if (isRunning.get()) {
            Log.d(TAG, "⚠️ Servidor ya corriendo")
            return
        }
        
        _serverState.value = ServerState.Starting
        
        scope.launch {
            try {
                // 1. Preparar APK
                val apk = prepareApk()
                if (apk == null) {
                    withContext(Dispatchers.Main) { onError("No se pudo preparar el APK") }
                    _serverState.value = ServerState.Error("APK no encontrado")
                    return@launch
                }
                apkFile = apk
                Log.d(TAG, "📦 APK listo: ${apk.length() / 1024 / 1024}MB")
                
                // 2. Crear hotspot
                val hotspotCreated = createLocalHotspot()
                
                if (!hotspotCreated) {
                    withContext(Dispatchers.Main) { onError("No se pudo crear el hotspot") }
                    _serverState.value = ServerState.Error("Hotspot no disponible")
                    return@launch
                }
                
                // 3. Esperar a que el hotspot esté listo
                delay(2000)
                
                // 4. Obtener IP del servidor
                val serverIp = getHotspotIp()
                Log.d(TAG, "🌐 IP del servidor: $serverIp")
                
                // 5. Iniciar servidor HTTP
                startHttpServer(langCode)
                
                val url = "http://$serverIp:$HTTP_PORT"
                _serverState.value = ServerState.Running(
                    url = url,
                    apkSize = apk.length(),
                    wifiName = hotspotSsid,
                    wifiPassword = hotspotPassword,
                    wifiQr = getWifiQrContent()
                )
                
                withContext(Dispatchers.Main) { onReady(url) }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error iniciando servidor: ${e.message}")
                _serverState.value = ServerState.Error(e.message ?: "Error desconocido")
                withContext(Dispatchers.Main) { onError(e.message ?: "Error") }
            }
        }
    }
    
    fun stop() {
        Log.d(TAG, "⏹️ Deteniendo servidor...")
        isRunning.set(false)
        
        serverJob?.cancel()
        runCatching { serverSocket?.close() }
        
        // Cerrar hotspot
        try {
            hotspotReservation?.close()
            hotspotReservation = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando hotspot: ${e.message}")
        }
        
        hotspotSsid = null
        hotspotPassword = null
        
        _serverState.value = ServerState.Stopped
        _downloadCount.value = 0
    }
    
    private fun prepareApk(): File? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            val sourceApk = File(appInfo.sourceDir)
            
            if (!sourceApk.exists()) return null
            
            // Copiar a cache para servir
            val cacheDir = File(context.cacheDir, "apk_server").apply { mkdirs() }
            val targetApk = File(cacheDir, "Orion.apk")
            
            sourceApk.inputStream().use { input ->
                targetApk.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            targetApk
        } catch (e: Exception) {
            Log.e(TAG, "Error preparando APK: ${e.message}")
            null
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun createLocalHotspot(): Boolean = suspendCancellableCoroutine { cont ->
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                        hotspotReservation = reservation
                        
                        val config = reservation.wifiConfiguration
                        if (config != null) {
                            hotspotSsid = config.SSID
                            @Suppress("DEPRECATION")
                            hotspotPassword = config.preSharedKey
                            Log.d(TAG, "✅ Hotspot creado: $hotspotSsid")
                            Log.d(TAG, "🔑 Contraseña: $hotspotPassword")
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // Android 11+ usa SoftApConfiguration
                            val softApConfig = reservation.softApConfiguration
                            hotspotSsid = softApConfig?.ssid
                            hotspotPassword = softApConfig?.passphrase
                            Log.d(TAG, "✅ Hotspot creado (R+): $hotspotSsid")
                        }
                        
                        cont.resume(true) {}
                    }
                    
                    override fun onStopped() {
                        Log.d(TAG, "⏹️ Hotspot detenido")
                        hotspotReservation = null
                    }
                    
                    override fun onFailed(reason: Int) {
                        Log.e(TAG, "❌ Error creando hotspot: $reason")
                        val errorMsg = when (reason) {
                            ERROR_NO_CHANNEL -> "No hay canal disponible"
                            ERROR_GENERIC -> "Error genérico"
                            ERROR_INCOMPATIBLE_MODE -> "Modo incompatible (¿WiFi Direct activo?)"
                            ERROR_TETHERING_DISALLOWED -> "Tethering no permitido"
                            else -> "Error desconocido ($reason)"
                        }
                        Log.e(TAG, "   Razón: $errorMsg")
                        cont.resume(false) {}
                    }
                }, Handler(Looper.getMainLooper()))
            } else {
                Log.e(TAG, "LocalOnlyHotspot requiere Android 8+")
                cont.resume(false) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando hotspot: ${e.message}")
            cont.resume(false) {}
        }
    }
    
    private fun getHotspotIp(): String {
        // La IP del hotspot local suele ser 192.168.43.1 o similar
        // Intentamos obtenerla de las interfaces de red
        try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { netInterface ->
                // Buscar interfaces de hotspot (ap, wlan, swlan)
                val name = netInterface.name.lowercase()
                if (name.contains("ap") || name.contains("swlan") || 
                    (name.contains("wlan") && netInterface.inetAddresses.toList().any { 
                        it is Inet4Address && it.hostAddress?.startsWith("192.168.43") == true 
                    })) {
                    netInterface.inetAddresses?.toList()?.forEach { addr ->
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            Log.d(TAG, "📡 IP hotspot encontrada en $name: ${addr.hostAddress}")
                            return addr.hostAddress ?: "192.168.43.1"
                        }
                    }
                }
            }
            
            // Buscar cualquier IP 192.168.43.x
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { netInterface ->
                netInterface.inetAddresses?.toList()?.forEach { addr ->
                    if (addr is Inet4Address && addr.hostAddress?.startsWith("192.168.43") == true) {
                        Log.d(TAG, "📡 IP 192.168.43.x encontrada: ${addr.hostAddress}")
                        return addr.hostAddress ?: "192.168.43.1"
                    }
                }
            }
            
            // Fallback: buscar cualquier IP local no loopback
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { netInterface ->
                netInterface.inetAddresses?.toList()?.forEach { addr ->
                    if (addr is Inet4Address && !addr.isLoopbackAddress && addr.hostAddress?.startsWith("192.168") == true) {
                        Log.d(TAG, "📡 IP fallback encontrada: ${addr.hostAddress}")
                        return addr.hostAddress ?: "192.168.43.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo IP: ${e.message}")
        }
        
        // Default para LocalOnlyHotspot
        return "192.168.43.1"
    }
    
    /**
     * Genera el código QR que conecta al WiFi
     * Formato estándar: WIFI:T:WPA;S:nombre;P:password;;
     */
    fun getWifiQrContent(): String? {
        val ssid = hotspotSsid ?: return null
        val pass = hotspotPassword ?: return null
        
        // Escapar caracteres especiales en SSID y password
        val escapedSsid = ssid.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\"", "\\\"").replace(":", "\\:")
        val escapedPass = pass.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\"", "\\\"").replace(":", "\\:")
        
        return "WIFI:T:WPA;S:$escapedSsid;P:$escapedPass;;"
    }
    
    fun getWifiName(): String? = hotspotSsid
    fun getWifiPassword(): String? = hotspotPassword
    
    private fun startHttpServer(langCode: String) {
        isRunning.set(true)
        
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(HTTP_PORT)
                serverSocket?.soTimeout = 0
                Log.d(TAG, "🚀 Servidor HTTP iniciado en puerto $HTTP_PORT")
                
                while (isRunning.get()) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        launch { handleClient(client, langCode) }
                    } catch (e: SocketException) {
                        if (isRunning.get()) Log.e(TAG, "Socket error: ${e.message}")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en servidor: ${e.message}")
            }
        }
    }
    
    private suspend fun handleClient(client: Socket, langCode: String) {
        withContext(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val output = client.getOutputStream()
                
                val requestLine = reader.readLine() ?: return@withContext
                Log.d(TAG, "📥 Request: $requestLine")
                
                // Consumir headers
                while (reader.readLine()?.isNotEmpty() == true) { }
                
                when {
                    requestLine.contains("GET /orion.apk") || 
                    requestLine.contains("GET /Orion.apk") ||
                    requestLine.contains("GET /download") -> {
                        serveApk(output)
                    }
                    else -> {
                        serveLandingPage(output, langCode)
                    }
                }
                
                client.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error manejando cliente: ${e.message}")
            }
        }
    }
    
    private fun serveLandingPage(output: OutputStream, langCode: String) {
        val apkSize = apkFile?.length() ?: 0
        val sizeMB = "%.1f".format(apkSize / 1024.0 / 1024.0)
        
        val html = generateLandingHtml(langCode, sizeMB)
        
        val response = """
HTTP/1.1 200 OK
Content-Type: text/html; charset=utf-8
Content-Length: ${html.toByteArray(Charsets.UTF_8).size}
Connection: close

$html""".trimIndent()
        
        output.write(response.toByteArray(Charsets.UTF_8))
        output.flush()
    }
    
    private fun serveApk(output: OutputStream) {
        val apk = apkFile ?: return
        
        Log.d(TAG, "📤 Enviando APK (${apk.length() / 1024 / 1024}MB)...")
        _downloadCount.value++
        
        val headers = """
HTTP/1.1 200 OK
Content-Type: application/vnd.android.package-archive
Content-Disposition: attachment; filename="Orion.apk"
Content-Length: ${apk.length()}
Connection: close

""".trimIndent()
        
        output.write(headers.toByteArray())
        
        apk.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalSent = 0L
            
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalSent += bytesRead
            }
        }
        
        output.flush()
        Log.d(TAG, "✅ APK enviado completamente")
    }
    
    private fun generateLandingHtml(langCode: String, sizeMB: String): String {
        val texts = when (langCode.lowercase()) {
            "es" -> LandingTexts(
                title = "📱 Instalar Orion",
                subtitle = "Comunicación de emergencia sin Internet",
                downloadBtn = "⬇️ DESCARGAR ORION",
                size = "Tamaño: $sizeMB MB",
                step1 = "1. Pulsa el botón verde",
                step2 = "2. Abre el archivo descargado",
                step3 = "3. Permite 'Instalar apps desconocidas'",
                step4 = "4. Instala y abre Orion",
                step5 = "5. Ve a Modo Emergencia",
                footer = "Una vez instalado, podrás comunicarte sin Internet"
            )
            "en" -> LandingTexts(
                title = "📱 Install Orion",
                subtitle = "Emergency communication without Internet",
                downloadBtn = "⬇️ DOWNLOAD ORION",
                size = "Size: $sizeMB MB",
                step1 = "1. Tap the green button",
                step2 = "2. Open the downloaded file",
                step3 = "3. Allow 'Install unknown apps'",
                step4 = "4. Install and open Orion",
                step5 = "5. Go to Emergency Mode",
                footer = "Once installed, you can communicate without Internet"
            )
            else -> LandingTexts(
                title = "📱 Install Orion",
                subtitle = "Emergency communication without Internet",
                downloadBtn = "⬇️ DOWNLOAD ORION",
                size = "Size: $sizeMB MB",
                step1 = "1. Tap the green button",
                step2 = "2. Open the downloaded file",
                step3 = "3. Allow 'Install unknown apps'",
                step4 = "4. Install and open Orion",
                step5 = "5. Go to Emergency Mode",
                footer = "Once installed, you can communicate without Internet"
            )
        }
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Orion</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
            min-height: 100vh;
            color: white;
            padding: 20px;
        }
        .container {
            max-width: 400px;
            margin: 0 auto;
            text-align: center;
        }
        .logo { font-size: 64px; margin: 20px 0; }
        h1 { font-size: 28px; margin-bottom: 8px; }
        .subtitle { color: #888; margin-bottom: 30px; }
        .download-btn {
            display: block;
            background: linear-gradient(135deg, #00E676 0%, #00C853 100%);
            color: #000;
            text-decoration: none;
            padding: 20px 32px;
            border-radius: 16px;
            font-size: 22px;
            font-weight: bold;
            margin: 20px 0;
            box-shadow: 0 4px 20px rgba(0, 230, 118, 0.4);
            animation: pulse 2s infinite;
        }
        @keyframes pulse {
            0%, 100% { transform: scale(1); box-shadow: 0 4px 20px rgba(0, 230, 118, 0.4); }
            50% { transform: scale(1.02); box-shadow: 0 4px 30px rgba(0, 230, 118, 0.6); }
        }
        .size { color: #666; font-size: 14px; margin-bottom: 30px; }
        .steps {
            background: rgba(255,255,255,0.05);
            border-radius: 12px;
            padding: 20px;
            text-align: left;
        }
        .step {
            padding: 12px 0;
            border-bottom: 1px solid rgba(255,255,255,0.1);
            font-size: 15px;
            color: #ccc;
        }
        .step:last-child { border-bottom: none; }
        .footer { margin-top: 30px; color: #00E676; font-size: 14px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">🛰️</div>
        <h1>${texts.title}</h1>
        <p class="subtitle">${texts.subtitle}</p>
        <a href="/download" class="download-btn">${texts.downloadBtn}</a>
        <p class="size">${texts.size}</p>
        <div class="steps">
            <div class="step">${texts.step1}</div>
            <div class="step">${texts.step2}</div>
            <div class="step">${texts.step3}</div>
            <div class="step">${texts.step4}</div>
            <div class="step">${texts.step5}</div>
        </div>
        <p class="footer">✨ ${texts.footer}</p>
    </div>
</body>
</html>
""".trimIndent()
    }
    
    private data class LandingTexts(
        val title: String,
        val subtitle: String,
        val downloadBtn: String,
        val size: String,
        val step1: String,
        val step2: String,
        val step3: String,
        val step4: String,
        val step5: String,
        val footer: String
    )
    
    sealed class ServerState {
        object Stopped : ServerState()
        object Starting : ServerState()
        data class Running(
            val url: String,
            val apkSize: Long,
            val wifiName: String?,
            val wifiPassword: String?,
            val wifiQr: String?
        ) : ServerState()
        data class Error(val message: String) : ServerState()
    }
}
