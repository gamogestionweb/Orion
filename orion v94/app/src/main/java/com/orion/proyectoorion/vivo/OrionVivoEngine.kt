package com.orion.proyectoorion.vivo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * OrionVivoEngine - Voz Duplex con Memoria
 */
class OrionVivoEngine(
    private val context: Context,
    private val apiKey: String,
    private val model: VivoModel = VivoModel.REALTIME_MINI,
    private val callback: OrionVivoCallback
) {

    companion object {
        private const val TAG = "OrionVivo"
        private const val SAMPLE_RATE = 24000
    }

    // Modelos disponibles
    enum class VivoModel(val id: String, val displayName: String, val priceInfo: String) {
        REALTIME("gpt-realtime", "GPT Realtime", "$40/80 por 1M tokens"),
        REALTIME_MINI("gpt-realtime-mini", "GPT Realtime Mini", "$10/20 por 1M tokens")
    }

    enum class VivoState {
        DORMANT, LISTENING, THINKING, SPEAKING, READY
    }

    private var currentState = MutableStateFlow(VivoState.DORMANT)
    val state: StateFlow<VivoState> = currentState.asStateFlow()

    // Memoria
    private var userMemory: UserMemory? = null
    private var memoryContext: String = ""

    // WebSocket
    private var webSocket: WebSocket? = null
    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // Audio
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private val isRecording = AtomicBoolean(false)
    private val isPlaying = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)
    private val isSpeaking = AtomicBoolean(false)

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ==========================================
    // INIT
    // ==========================================

    fun initialize() {
        Log.d(TAG, "Inicializando OrionVivo - Modelo: ${model.displayName}")
        loadMemory()
        callback.onStateChanged(VivoState.DORMANT)
    }

    // ==========================================
    // MEMORIA - CARGA ROBUSTA
    // ==========================================

    private fun loadMemory() {
        Log.d(TAG, "Buscando memoria...")
        
        try {
            // Lista de ubicaciones donde buscar el JSON
            val locations = buildList {
                // App internal storage
                add(File(context.filesDir, "memory.json"))
                add(File(context.filesDir, "orion_memory.json"))
                
                // App external storage
                context.getExternalFilesDir(null)?.let {
                    add(File(it, "memory.json"))
                    add(File(it, "orion_memory.json"))
                }
                
                // Downloads
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                add(File(downloads, "memory.json"))
                add(File(downloads, "orion_memory.json"))
                
                // Documents  
                val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                add(File(documents, "memory.json"))
                add(File(documents, "orion_memory.json"))
                
                // Root of external storage
                add(File(Environment.getExternalStorageDirectory(), "memory.json"))
                add(File(Environment.getExternalStorageDirectory(), "orion_memory.json"))
            }

            // Buscar en archivos
            for (file in locations) {
                try {
                    if (file.exists() && file.canRead()) {
                        val content = file.readText().trim()
                        if (content.startsWith("{")) {
                            userMemory = parseMemoryJson(content)
                            memoryContext = buildMemoryContext()
                            Log.d(TAG, "✓ Memoria encontrada: ${file.absolutePath}")
                            logMemoryDetails()
                            callback.onMemoryLoaded(userMemory)
                            return
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo leer: ${file.absolutePath}")
                }
            }

            // Buscar en assets
            try {
                val assetContent = context.assets.open("memory.json").bufferedReader().readText()
                userMemory = parseMemoryJson(assetContent)
                memoryContext = buildMemoryContext()
                Log.d(TAG, "✓ Memoria desde assets")
                logMemoryDetails()
                callback.onMemoryLoaded(userMemory)
                return
            } catch (_: Exception) {}

            // SharedPreferences como último recurso
            val prefs = context.getSharedPreferences("orion_prefs", Context.MODE_PRIVATE)
            prefs.getString("memory_json", null)?.let { json ->
                userMemory = parseMemoryJson(json)
                memoryContext = buildMemoryContext()
                Log.d(TAG, "✓ Memoria desde SharedPreferences")
                logMemoryDetails()
                callback.onMemoryLoaded(userMemory)
                return
            }

            Log.w(TAG, "⚠ No se encontró archivo de memoria")
            callback.onMemoryLoaded(null)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando memoria", e)
            callback.onMemoryLoaded(null)
        }
    }

    private fun parseMemoryJson(json: String): UserMemory {
        val obj = JSONObject(json)
        
        // Extraer nombre (varios formatos posibles)
        val name = obj.optString("userName", 
            obj.optString("name",
                obj.optString("user_name",
                    obj.optString("username", ""))))
        
        // Extraer facts/memories
        val facts = mutableListOf<String>()
        
        // Formato array directo
        listOf("facts", "memories", "data", "info").forEach { key ->
            obj.optJSONArray(key)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.opt(i)
                    when (item) {
                        is String -> if (item.isNotBlank()) facts.add(item)
                        is JSONObject -> {
                            // Formato {"fact": "..."} o {"memory": "..."}
                            item.optString("fact", item.optString("memory", item.optString("text", "")))
                                .takeIf { it.isNotBlank() }?.let { facts.add(it) }
                        }
                    }
                }
            }
        }
        
        // Formato objeto con campos individuales
        obj.keys().forEach { key ->
            if (key !in listOf("userName", "name", "user_name", "username", "facts", "memories", 
                              "data", "info", "preferences", "language", "lang")) {
                val value = obj.opt(key)
                if (value is String && value.isNotBlank()) {
                    facts.add("$key: $value")
                }
            }
        }

        return UserMemory(
            userName = name,
            facts = facts,
            preferences = obj.optString("preferences", ""),
            language = obj.optString("language", obj.optString("lang", "es"))
        )
    }

    private fun buildMemoryContext(): String {
        val m = userMemory ?: return ""
        return buildString {
            if (m.userName.isNotBlank()) {
                append("El usuario se llama ${m.userName}. ")
            }
            if (m.facts.isNotEmpty()) {
                append("Lo que sé: ${m.facts.joinToString(". ")}. ")
            }
            if (m.preferences.isNotBlank()) {
                append("Preferencias: ${m.preferences}")
            }
        }.trim()
    }

    private fun logMemoryDetails() {
        userMemory?.let { m ->
            Log.d(TAG, "  → Nombre: ${m.userName}")
            Log.d(TAG, "  → Facts: ${m.facts.size}")
            m.facts.take(3).forEach { Log.d(TAG, "    - $it") }
            if (m.facts.size > 3) Log.d(TAG, "    ... y ${m.facts.size - 3} más")
        }
    }

    /** Permite establecer memoria manualmente desde la app */
    fun setUserMemory(memory: UserMemory) {
        userMemory = memory
        memoryContext = buildMemoryContext()
        Log.d(TAG, "Memoria establecida manualmente: ${memory.userName}")
    }

    /** Permite cargar memoria desde un JSON string */
    fun loadMemoryFromJson(json: String) {
        try {
            userMemory = parseMemoryJson(json)
            memoryContext = buildMemoryContext()
            Log.d(TAG, "Memoria cargada desde JSON")
            logMemoryDetails()
            callback.onMemoryLoaded(userMemory)
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando JSON", e)
        }
    }

    /** Guarda la memoria en SharedPreferences para persistencia */
    fun saveMemoryToPrefs() {
        userMemory?.let { m ->
            val json = JSONObject().apply {
                put("userName", m.userName)
                put("facts", JSONArray(m.facts))
                put("preferences", m.preferences)
                put("language", m.language)
            }.toString()
            
            context.getSharedPreferences("orion_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("memory_json", json)
                .apply()
            
            Log.d(TAG, "Memoria guardada en prefs")
        }
    }

    // ==========================================
    // START / STOP
    // ==========================================

    fun start() {
        scope.launch {
            try {
                Log.d(TAG, "Conectando a ${model.id}...")
                connectWebSocket()
                delay(800)
                startAudioCapture()
                startAudioPlayback()
                currentState.value = VivoState.READY
                callback.onStateChanged(VivoState.READY)
                Log.d(TAG, "✓ Activo")
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
                callback.onError("Error: ${e.message}")
            }
        }
    }

    fun stop() {
        scope.launch {
            stopAudioCapture()
            stopAudioPlayback()
            disconnectWebSocket()
            currentState.value = VivoState.DORMANT
            callback.onStateChanged(VivoState.DORMANT)
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }

    // ==========================================
    // WEBSOCKET
    // ==========================================

    private suspend fun connectWebSocket() {
        withContext(Dispatchers.IO) {
            val url = "wss://api.openai.com/v1/realtime?model=${model.id}"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("OpenAI-Beta", "realtime=v1")
                .build()

            webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "✓ WebSocket conectado")
                    scope.launch { configureSession() }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    scope.launch { handleEvent(text) }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Error WS: ${t.message}")
                    callback.onError("Conexión perdida")
                    scope.launch {
                        delay(2000)
                        if (currentState.value != VivoState.DORMANT) connectWebSocket()
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WS cerrado: $reason")
                }
            })
        }
    }

    private fun configureSession() {
        val config = JSONObject().apply {
            put("type", "session.update")
            put("session", JSONObject().apply {
                put("modalities", JSONArray().put("text").put("audio"))
                put("instructions", buildSystemPrompt())
                put("voice", "cedar")
                put("input_audio_format", "pcm16")
                put("output_audio_format", "pcm16")
                put("input_audio_transcription", JSONObject().put("model", "whisper-1"))
                put("turn_detection", JSONObject().apply {
                    put("type", "server_vad")
                    put("threshold", 0.5)
                    put("prefix_padding_ms", 200)
                    put("silence_duration_ms", 500)
                    put("create_response", true)
                })
            })
        }
        webSocket?.send(config.toString())
        Log.d(TAG, "Sesión configurada con memoria: ${memoryContext.take(100)}...")
    }

    private fun buildSystemPrompt(): String {
        val name = userMemory?.userName?.takeIf { it.isNotBlank() } ?: "usuario"
        
        return buildString {
            append("Eres Orion, el compañero de IA de $name.\n\n")
            
            if (memoryContext.isNotBlank()) {
                append("=== MEMORIA ===\n")
                append(memoryContext)
                append("\n\n")
            }
            
            append("""
=== PERSONALIDAD ===
- Eres un amigo cercano, NO un asistente
- Hablas natural, breve y directo
- Tienes humor y empatía
- Usas el nombre "$name" de forma natural

=== REGLAS ===
1. SIEMPRE en español
2. Máximo 2-3 oraciones
3. NO repitas lo que dice $name
4. NO digas "¿en qué puedo ayudarte?"
5. USA la memoria - recuerda quién es y qué le gusta
6. Si te interrumpen, PARA inmediatamente
            """.trimIndent())
        }
    }

    private suspend fun handleEvent(json: String) {
        try {
            val event = JSONObject(json)
            when (event.optString("type")) {
                
                "input_audio_buffer.speech_started" -> {
                    if (isSpeaking.get()) cancelResponse()
                    currentState.value = VivoState.LISTENING
                    callback.onStateChanged(VivoState.LISTENING)
                }

                "input_audio_buffer.speech_stopped" -> {
                    currentState.value = VivoState.THINKING
                    callback.onStateChanged(VivoState.THINKING)
                }

                "response.audio.delta" -> {
                    event.optString("delta").takeIf { it.isNotEmpty() }?.let { delta ->
                        if (!isSpeaking.get()) {
                            isSpeaking.set(true)
                            currentState.value = VivoState.SPEAKING
                            callback.onStateChanged(VivoState.SPEAKING)
                        }
                        try {
                            audioQueue.send(Base64.decode(delta, Base64.DEFAULT))
                        } catch (_: Exception) {}
                    }
                }

                "response.done" -> {
                    scope.launch {
                        delay(200)
                        while (!audioQueue.isEmpty) delay(50)
                        delay(100)
                        isSpeaking.set(false)
                        currentState.value = VivoState.READY
                        callback.onStateChanged(VivoState.READY)
                    }
                }

                "error" -> {
                    val msg = event.optJSONObject("error")?.optString("message") ?: "Error"
                    Log.e(TAG, "Error: $msg")
                    if (!msg.contains("cancel", true) && !msg.contains("interrupt", true)) {
                        callback.onError(msg)
                    }
                    isSpeaking.set(false)
                    currentState.value = VivoState.READY
                    callback.onStateChanged(VivoState.READY)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evento", e)
        }
    }

    private fun cancelResponse() {
        webSocket?.send("""{"type":"response.cancel"}""")
        while (!audioQueue.isEmpty) audioQueue.tryReceive()
        isSpeaking.set(false)
    }

    private fun disconnectWebSocket() {
        webSocket?.close(1000, "Bye")
        webSocket = null
    }

    // ==========================================
    // AUDIO
    // ==========================================

    private fun startAudioCapture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            callback.onError("Permiso de micrófono requerido")
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2)
        } catch (_: Exception) {
            AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2)
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            callback.onError("No se pudo iniciar micrófono")
            return
        }

        isRecording.set(true)
        audioRecord?.startRecording()

        recordingJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(bufferSize)
            while (isRecording.get()) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    // Solo enviar audio si NO está hablando (evitar eco/auto-respuesta)
                    if (!isSpeaking.get()) {
                        sendAudio(buffer.copyOf(read))
                    }
                }
            }
        }
    }

    private fun sendAudio(data: ByteArray) {
        try {
            webSocket?.send(JSONObject().apply {
                put("type", "input_audio_buffer.append")
                put("audio", Base64.encodeToString(data, Base64.NO_WRAP))
            }.toString())
        } catch (_: Exception) {}
    }

    private fun stopAudioCapture() {
        isRecording.set(false)
        recordingJob?.cancel()
        runCatching { audioRecord?.stop(); audioRecord?.release() }
        audioRecord = null
    }

    private fun startAudioPlayback() {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        isPlaying.set(true)
        audioTrack?.play()

        playbackJob = scope.launch(Dispatchers.IO) {
            while (isPlaying.get()) {
                audioQueue.tryReceive().getOrNull()?.let { data ->
                    audioTrack?.write(data, 0, data.size)
                } ?: delay(10)
            }
        }
    }

    private fun stopAudioPlayback() {
        isPlaying.set(false)
        playbackJob?.cancel()
        runCatching { audioTrack?.stop(); audioTrack?.release() }
        audioTrack = null
    }

    // ==========================================
    // PUBLIC API
    // ==========================================

    fun interrupt() {
        if (isSpeaking.get()) {
            cancelResponse()
            currentState.value = VivoState.READY
            callback.onStateChanged(VivoState.READY)
        }
    }

    fun getMemory() = userMemory
    fun getModel() = model
    fun isActive() = currentState.value != VivoState.DORMANT
}

// ==========================================
// DATA CLASSES
// ==========================================

data class UserMemory(
    val userName: String = "",
    val facts: List<String> = emptyList(),
    val preferences: String = "",
    val language: String = "es"
)

interface OrionVivoCallback {
    fun onStateChanged(state: OrionVivoEngine.VivoState)
    fun onError(error: String)
    fun onMemoryLoaded(memory: UserMemory?)
}
