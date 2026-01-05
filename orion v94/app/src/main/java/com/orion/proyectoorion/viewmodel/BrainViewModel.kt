package com.orion.proyectoorion.viewmodel

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.orion.proyectoorion.ai.LocalLLMEngine
import com.orion.proyectoorion.ai.LLMCallback
import com.orion.proyectoorion.data.*
import com.orion.proyectoorion.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

// ==========================================================
// ESTADOS DE PROCESAMIENTO
// ==========================================================

enum class ProcessingState { IDLE, THINKING, ANALYZING, RESPONDING }

// ==========================================================
// BRAIN VIEW MODEL
// ==========================================================

class BrainViewModel : ViewModel() {

    private var cloudModel: GenerativeModel? = null
    private var localEngine: LocalLLMEngine? = null
    private var dataManager: DataManager? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val generationMutex = Mutex()
    private var currentGenerationJob: Job? = null

    // Estado de UI
    var uiStrings by mutableStateOf(LANG_ES)
    var messages = mutableStateListOf<Pair<String, Boolean>>()
    var memoryJson by mutableStateOf("""{"memorias": []}""")
    var isLoading by mutableStateOf(false)
    var isProcessingMemory by mutableStateOf(false)
    var isLocalMode by mutableStateOf(false)
    var isLoadingModel by mutableStateOf(false)
    var processingState by mutableStateOf(ProcessingState.IDLE)
    var downloadingModelId by mutableStateOf<String?>(null)
    var downloadProgress by mutableStateOf(0f)
    var selectedModel by mutableStateOf<AvailableModel?>(null)
    var activeCloudProvider by mutableStateOf<CloudProvider?>(null)
    var selectedCloudModels = mutableStateMapOf<CloudProvider, CloudModel>()

    private var currentResponse = StringBuilder()
    private val conversationHistory = mutableListOf<Pair<String, String>>()
    private val maxHistoryTurns = 5

    // ==========================================================
    // INICIALIZACIÓN
    // ==========================================================

    fun init(context: Context) {
        if (dataManager == null) {
            dataManager = DataManager(context)
            localEngine = LocalLLMEngine(context)

            val rawMemory = dataManager!!.leerMemoria()
            memoryJson = migrateMemoryFormat(rawMemory)
            if (memoryJson != rawMemory) {
                dataManager!!.guardarMemoria(memoryJson)
            }

            // Cargar modelos cloud seleccionados
            CloudProvider.entries.forEach { provider ->
                val savedModelId = dataManager!!.getSelectedCloudModel(provider)
                val model = if (savedModelId != null) {
                    provider.models.find { it.id == savedModelId } ?: provider.getDefaultModel()
                } else {
                    provider.getDefaultModel()
                }
                selectedCloudModels[provider] = model
            }
        }
    }

    /**
     * Verifica si hay un motor válido configurado
     */
    fun hasValidEngine(): Boolean {
        val dm = dataManager ?: return false
        
        if (!dm.isLanguageConfigured()) return false
        
        val savedLocal = dm.isLocal()
        val savedModelId = dm.getSelectedModel()
        val savedProvider = dm.getActiveProvider()

        return if (savedLocal && savedModelId != null) {
            val model = AVAILABLE_MODELS.find { it.id == savedModelId }
            model != null && dm.isModelDownloaded(model)
        } else if (!savedLocal && savedProvider != null) {
            !dm.getApiKey(savedProvider).isNullOrBlank()
        } else {
            false
        }
    }

    /**
     * Restaura el estado guardado y devuelve true si hay un motor válido
     */
    fun restoreState(context: Context): Boolean {
        init(context)
        val dm = dataManager ?: return false

        if (dm.isLanguageConfigured()) {
            setLangInternal(dm.getLang() ?: "ES")
            val savedLocal = dm.isLocal()
            val savedModelId = dm.getSelectedModel()
            val savedProvider = dm.getActiveProvider()

            if (savedLocal && savedModelId != null) {
                isLocalMode = true
                selectedModel = AVAILABLE_MODELS.find { it.id == savedModelId }
                if (selectedModel != null && dm.isModelDownloaded(selectedModel!!)) {
                    viewModelScope.launch { loadLocalModel(context) }
                    return true
                }
            } else if (!savedLocal && savedProvider != null) {
                val savedKey = dm.getApiKey(savedProvider)
                if (!savedKey.isNullOrBlank()) {
                    isLocalMode = false
                    activeCloudProvider = savedProvider
                    initCloudProvider(savedProvider, savedKey)
                    return true
                }
            }
        }
        return false
    }

    // ==========================================================
    // MIGRACIÓN DE MEMORIA
    // ==========================================================

    private fun migrateMemoryFormat(rawJson: String): String {
        return try {
            val root = JSONObject(rawJson)
            if (root.has("memorias") && !root.has("perfil") && !root.has("recuerdos")) {
                return rawJson
            }
            val newMemorias = JSONArray()
            root.optJSONObject("perfil")?.let { perfil ->
                perfil.keys().forEach { key ->
                    val value = perfil.optString(key, "")
                    if (value.isNotBlank()) {
                        val readable = convertToReadable(key, value)
                        if (readable.isNotBlank()) newMemorias.put(readable)
                    }
                }
            }
            root.optJSONArray("recuerdos")?.let { recuerdos ->
                for (i in 0 until recuerdos.length()) {
                    val recuerdo = recuerdos.optString(i, "")
                    val cleaned = cleanMemoryText(recuerdo)
                    if (cleaned.isNotBlank() && !isDuplicate(newMemorias, cleaned)) {
                        newMemorias.put(cleaned)
                    }
                }
            }
            root.optJSONArray("memorias")?.let { memorias ->
                for (i in 0 until memorias.length()) {
                    val mem = memorias.optString(i, "")
                    val cleaned = cleanMemoryText(mem)
                    if (cleaned.isNotBlank() && !isDuplicate(newMemorias, cleaned)) {
                        newMemorias.put(cleaned)
                    }
                }
            }
            JSONObject().put("memorias", newMemorias).toString()
        } catch (e: Exception) {
            """{"memorias": []}"""
        }
    }

    private fun convertToReadable(key: String, value: String): String {
        val cleanValue = cleanMemoryText(value)
        return when (key.lowercase()) {
            "nombre", "name" -> "Se llama $cleanValue"
            "edad", "age" -> "Tiene $cleanValue años"
            "trabajo", "job", "profesión", "profession" -> "Trabaja como $cleanValue"
            "ciudad", "city" -> "Vive en $cleanValue"
            "mascota", "pet" -> "Tiene una mascota: $cleanValue"
            else -> if (cleanValue.isNotBlank()) "$key: $cleanValue" else ""
        }
    }

    private fun cleanMemoryText(text: String): String {
        var cleaned = text
        cleaned = cleaned.replace(Regex("""^\{.*?":\s*"""), "")
        cleaned = cleaned.replace(Regex("""\}$"""), "")
        cleaned = cleaned.replace(Regex("""^\["""), "")
        cleaned = cleaned.replace(Regex("""\]$"""), "")
        cleaned = cleaned.replace(Regex(""""[^"]*":\s*"""), "")
        cleaned = cleaned.replace("\"", "")
        cleaned = cleaned.replace(Regex("""\{[^}]*\}"""), "")
        cleaned = cleaned.replace("\\n", " ")
        cleaned = cleaned.replace("\\t", " ")
        cleaned = cleaned.replace("\\", "")
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()
        return cleaned
    }

    private fun isDuplicate(array: JSONArray, text: String): Boolean {
        val textLower = text.lowercase()
        for (i in 0 until array.length()) {
            if (array.optString(i, "").lowercase().contains(textLower) ||
                textLower.contains(array.optString(i, "").lowercase())) {
                return true
            }
        }
        return false
    }

    // ==========================================================
    // IDIOMA
    // ==========================================================

    private fun setLangInternal(code: String) {
        uiStrings = getStringsForLanguage(code)
    }

    fun setLang(code: String) {
        setLangInternal(code)
        dataManager?.saveLang(code)
    }

    fun isLanguageConfigured(): Boolean = dataManager?.isLanguageConfigured() ?: false

    // ==========================================================
    // SELECCIÓN DE MOTOR
    // ==========================================================

    fun selectEngine(local: Boolean) {
        isLocalMode = local
        dataManager?.saveMode(local)
    }

    fun selectCloudProvider(provider: CloudProvider) {
        activeCloudProvider = provider
        val savedKey = dataManager?.getApiKey(provider)
        if (!savedKey.isNullOrBlank()) {
            dataManager?.saveActiveProvider(provider)
            initCloudProvider(provider, savedKey)
        }
    }

    fun hasApiKeyFor(provider: CloudProvider): Boolean {
        return dataManager?.hasApiKey(provider) ?: false
    }

    fun selectCloudModel(provider: CloudProvider, model: CloudModel) {
        selectedCloudModels[provider] = model
        dataManager?.saveSelectedCloudModel(provider, model.id)
        if (provider == activeCloudProvider && provider == CloudProvider.GEMINI) {
            val apiKey = dataManager?.getApiKey(provider)
            if (!apiKey.isNullOrBlank()) {
                cloudModel = GenerativeModel(model.apiModelId, apiKey)
            }
        }
    }

    fun getSelectedCloudModel(provider: CloudProvider): CloudModel {
        return selectedCloudModels[provider] ?: provider.getDefaultModel()
    }

    fun setupApiKey(key: String) {
        val provider = activeCloudProvider ?: return
        if (key.length > 10) {
            dataManager?.saveApiKey(provider, key)
            dataManager?.saveActiveProvider(provider)
            dataManager?.saveMode(false)
            initCloudProvider(provider, key)
        }
    }

    private fun initCloudProvider(provider: CloudProvider, apiKey: String) {
        when (provider) {
            CloudProvider.GEMINI -> {
                val model = getSelectedCloudModel(provider)
                cloudModel = GenerativeModel(model.apiModelId, apiKey)
            }
            else -> cloudModel = null
        }
        if (messages.isEmpty()) messages.add(uiStrings.systemWelcome to false)
    }

    // ==========================================================
    // CHAT
    // ==========================================================

    fun startNewChat() {
        currentGenerationJob?.cancel()
        messages.clear()
        conversationHistory.clear()
        currentResponse.clear()
        processingState = ProcessingState.IDLE
        isLoading = false
        messages.add(uiStrings.systemWelcome to false)
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank() || isLoading) return
        
        // Detectar comandos directos de memoria ANTES de enviar
        val directCommand = detectDirectMemoryCommand(prompt)
        if (directCommand != null) {
            processDirectMemoryCommand(directCommand)
            // Si es solo un comando de memoria, dar confirmación inmediata
            val confirmMsg = when (directCommand.first) {
                "add" -> when (uiStrings.langCode) {
                    "ES" -> "✓ Guardado en memoria: ${directCommand.second}"
                    "EN" -> "✓ Saved to memory: ${directCommand.second}"
                    "FR" -> "✓ Enregistré en mémoire: ${directCommand.second}"
                    else -> "✓ Saved: ${directCommand.second}"
                }
                "remove" -> when (uiStrings.langCode) {
                    "ES" -> "✓ Eliminado de memoria"
                    "EN" -> "✓ Removed from memory"
                    "FR" -> "✓ Supprimé de la mémoire"
                    else -> "✓ Removed"
                }
                else -> null
            }
            if (confirmMsg != null && prompt.length < 100) {
                // Solo si es un comando corto y directo, dar feedback inmediato
                messages.add(prompt to true)
                messages.add(confirmMsg to false)
                return
            }
        }
        
        messages.add(prompt to true)
        isLoading = true
        processingState = ProcessingState.THINKING
        currentResponse.clear()

        currentGenerationJob = viewModelScope.launch {
            try {
                generationMutex.withLock {
                    val mem = buildMemoryContext()
                    if (isLocalMode) {
                        sendLocalMessage(prompt, mem)
                    } else {
                        val sys = buildSystemPrompt()
                        val resp = when (activeCloudProvider) {
                            CloudProvider.GEMINI -> sendGeminiMessage(prompt, sys, mem)
                            CloudProvider.OPENAI -> sendOpenAIMessage(prompt, sys, mem)
                            CloudProvider.CLAUDE -> sendClaudeMessage(prompt, sys, mem)
                            CloudProvider.DEEPSEEK -> sendDeepSeekMessage(prompt, sys, mem)
                            CloudProvider.QWEN -> sendQwenMessage(prompt, sys, mem)
                            CloudProvider.KIMI -> sendKimiMessage(prompt, sys, mem)
                            null -> uiStrings.errorConnection
                        }
                        messages.add(resp to false)
                        if (resp.isNotBlank() && !resp.startsWith("Error")) {
                            conversationHistory.add(prompt to resp)
                            if (conversationHistory.size > maxHistoryTurns) conversationHistory.removeAt(0)
                            processingState = ProcessingState.ANALYZING
                            processMemoryUpdate(prompt, resp)
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Cancelled
            } catch (e: Exception) {
                messages.add((if (currentResponse.isNotEmpty()) currentResponse.toString() else "Error: ${e.message}") to false)
            } finally {
                isLoading = false
                processingState = ProcessingState.IDLE
            }
        }
    }

    // ==========================================================
    // CONTEXTO Y PROMPTS
    // ==========================================================

    private fun buildMemoryContext(): String {
        return try {
            val root = JSONObject(memoryJson)
            val memorias = root.optJSONArray("memorias") ?: return ""
            if (memorias.length() == 0) return ""
            val sb = StringBuilder()
            sb.append("Información conocida sobre el usuario: ")
            val items = mutableListOf<String>()
            for (i in 0 until minOf(memorias.length(), 10)) {
                val mem = memorias.optString(i, "")
                if (mem.isNotBlank()) items.add(mem)
            }
            sb.append(items.joinToString(". "))
            sb.append(".")
            sb.toString()
        } catch (e: Exception) { "" }
    }

    private fun buildSystemPrompt(): String {
        return """Eres Orion, un asistente de IA personal amigable y útil. 
Idioma de respuesta: ${uiStrings.langName}. 
Responde de forma natural, concisa y en el idioma del usuario.
IMPORTANTE: Cuando el usuario te dé información personal (nombre, trabajo, gustos, etc.) o corrija información anterior, recuérdalo para futuras conversaciones."""
    }

    // ==========================================================
    // MODELOS LOCALES
    // ==========================================================

    fun selectAndLoadModel(context: Context, model: AvailableModel) {
        selectedModel = model
        dataManager?.saveSelectedModel(model.id)
        if (dataManager?.isModelDownloaded(model) == true) {
            viewModelScope.launch { loadLocalModel(context) }
        }
    }

    fun downloadModel(context: Context, model: AvailableModel) {
        downloadingModelId = model.id
        downloadProgress = 0f
        
        val request = DownloadManager.Request(Uri.parse(model.downloadUrl))
            .setTitle("Orion - ${model.name}")
            .setDescription("Descargando modelo de IA")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "orion_temp_${model.fileName}")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
        
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        viewModelScope.launch(Dispatchers.IO) {
            var downloading = true
            while (downloading) {
                val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor.moveToFirst()) {
                    val si = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bi = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val ti = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    if (si >= 0 && bi >= 0 && ti >= 0) {
                        val total = cursor.getLong(ti)
                        if (total > 0) downloadProgress = cursor.getLong(bi).toFloat() / total
                        when (cursor.getInt(si)) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloading = false
                                moveDownloadedModel(context, model)
                                withContext(Dispatchers.Main) { 
                                    downloadingModelId = null
                                    downloadProgress = 0f 
                                }
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloading = false
                                withContext(Dispatchers.Main) { 
                                    downloadingModelId = null
                                    downloadProgress = 0f 
                                }
                            }
                        }
                    }
                }
                cursor.close()
                delay(500)
            }
        }
    }

    private fun moveDownloadedModel(context: Context, model: AvailableModel) {
        try {
            val pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val src = File(pub, "orion_temp_${model.fileName}")
            val dst = File(dataManager!!.getModelsDir(), model.fileName)
            if (src.exists()) {
                src.inputStream().use { i -> dst.outputStream().use { o -> i.copyTo(o) } }
                src.delete()
            }
        } catch (e: Exception) { 
            e.printStackTrace() 
        }
    }

    private suspend fun loadLocalModel(context: Context) {
        val model = selectedModel ?: return
        val path = dataManager?.getModelPath(model) ?: return
        if (!File(path).exists()) return

        isLoadingModel = true
        localEngine?.release()
        val success = localEngine?.loadModel(modelPath = path, threads = 4, contextSize = 4096) ?: false
        isLoadingModel = false

        if (success) {
            if (messages.isEmpty()) messages.add(uiStrings.systemWelcome to false)
        } else {
            messages.add(uiStrings.errorLoadModel to false)
        }
    }

    fun deleteModel(model: AvailableModel) {
        dataManager?.deleteModel(model)
        if (selectedModel?.id == model.id) {
            selectedModel = null
            dataManager?.saveSelectedModel("")
        }
    }

    fun isModelDownloaded(model: AvailableModel): Boolean {
        return dataManager?.isModelDownloaded(model) ?: false
    }

    // ==========================================================
    // GENERACIÓN LOCAL
    // ==========================================================

    private suspend fun sendLocalMessage(prompt: String, memory: String): String {
        val model = selectedModel ?: return ""
        processingState = ProcessingState.RESPONDING
        val fullPrompt = buildModelPrompt(model, prompt, memory)
        val responseIndex = messages.size
        messages.add("" to false)
        val result = CompletableDeferred<String>()

        localEngine?.generate(
            prompt = fullPrompt,
            maxTokens = 768,
            callback = object : LLMCallback {
                override fun onToken(token: String) {
                    for (st in model.stopTokens) {
                        if (currentResponse.toString().contains(st)) {
                            currentResponse.clear().append(currentResponse.toString().replace(st, "").trim())
                            localEngine?.stopGeneration()
                            return
                        }
                    }
                    currentResponse.append(token)
                    val disp = cleanModelResponse(currentResponse.toString())
                    if (responseIndex < messages.size) messages[responseIndex] = disp to false
                }
                override fun onComplete() {
                    val final = cleanModelResponse(currentResponse.toString())
                    if (responseIndex < messages.size) messages[responseIndex] = final to false
                    result.complete(final)
                }
                override fun onError(error: String) {
                    val cur = currentResponse.toString()
                    if (responseIndex < messages.size) {
                        messages[responseIndex] = (if (cur.isNotEmpty()) cleanModelResponse(cur) else "Error: $error") to false
                    }
                    result.complete(cur)
                }
            }
        )
        return try { 
            withTimeout(90000) { result.await() } 
        } catch (e: TimeoutCancellationException) { 
            localEngine?.stopGeneration()
            cleanModelResponse(currentResponse.toString()) 
        }
    }

    private fun buildModelPrompt(model: AvailableModel, userMessage: String, memory: String): String {
        return when (model.chatTemplate) {
            ChatTemplate.CHATML -> buildChatMLPrompt(userMessage, memory)
            ChatTemplate.GEMMA3 -> buildGemma3Prompt(userMessage, memory)
            ChatTemplate.LLAMA3 -> buildLlama3Prompt(userMessage, memory)
            ChatTemplate.PHI3 -> buildPhi3Prompt(userMessage, memory)
            ChatTemplate.NEMOTRON -> buildNemotronPrompt(userMessage, memory)
        }
    }

    private fun buildChatMLPrompt(userMessage: String, memory: String): String {
        val sys = "Eres Orion, asistente personal. Responde en ${uiStrings.langName}." + if (memory.isNotBlank()) "\n\n$memory" else ""
        val hist = conversationHistory.takeLast(2).joinToString("") { 
            "<|im_start|>user\n${it.first.take(200)}<|im_end|>\n<|im_start|>assistant\n${it.second.take(300)}<|im_end|>\n" 
        }
        return "<|im_start|>system\n$sys<|im_end|>\n$hist<|im_start|>user\n$userMessage<|im_end|>\n<|im_start|>assistant\n"
    }

    private fun buildGemma3Prompt(userMessage: String, memory: String): String {
        val sys = "Eres Orion, asistente personal. Responde en ${uiStrings.langName}." + if (memory.isNotBlank()) "\n\n$memory" else ""
        val hist = conversationHistory.takeLast(2).joinToString("") { 
            "<start_of_turn>user\n${it.first.take(200)}<end_of_turn>\n<start_of_turn>model\n${it.second.take(300)}<end_of_turn>\n" 
        }
        return "<start_of_turn>user\n$sys\n\n$hist$userMessage<end_of_turn>\n<start_of_turn>model\n"
    }

    private fun buildLlama3Prompt(userMessage: String, memory: String): String {
        val sys = "Eres Orion, asistente personal. Responde en ${uiStrings.langName}." + if (memory.isNotBlank()) " $memory" else ""
        val hist = conversationHistory.takeLast(2).joinToString("") { 
            "<|start_header_id|>user<|end_header_id|>\n\n${it.first.take(200)}<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n${it.second.take(300)}<|eot_id|>" 
        }
        return "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$sys<|eot_id|>$hist<|start_header_id|>user<|end_header_id|>\n\n$userMessage<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
    }

    private fun buildPhi3Prompt(userMessage: String, memory: String): String {
        val sys = "Eres Orion, asistente personal. Responde en ${uiStrings.langName}." + if (memory.isNotBlank()) " $memory" else ""
        return "<|system|>\n$sys<|end|>\n<|user|>\n$userMessage<|end|>\n<|assistant|>\n"
    }

    private fun buildNemotronPrompt(userMessage: String, memory: String): String {
        val sys = "Eres Orion, asistente personal. Responde en ${uiStrings.langName}." + if (memory.isNotBlank()) " $memory" else ""
        return "<extra_id_0>System\n$sys\n<extra_id_1>User\n$userMessage\n<extra_id_1>Assistant\n"
    }

    private fun cleanModelResponse(response: String): String {
        var c = response.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
        listOf("<|im_start|>", "<|im_end|>", "<|endoftext|>", "<|eot_id|>", "<|end_of_text|>",
            "<|start_header_id|>", "<|end_header_id|>", "<|begin_of_text|>", "<|end|>",
            "<|user|>", "<|assistant|>", "<|system|>", "<start_of_turn>", "<end_of_turn>",
            "<eos>", "<extra_id_0>", "<extra_id_1>", "assistant", "user", "system",
            "Assistant", "User", "System", "model"
        ).forEach { c = c.replace(it, "") }
        return c.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    // ==========================================================
    // GENERACIÓN CLOUD
    // ==========================================================

    private suspend fun sendGeminiMessage(prompt: String, system: String, memory: String): String {
        processingState = ProcessingState.RESPONDING
        return withContext(Dispatchers.IO) {
            cloudModel?.generateContent("$system\n\n$memory\n\nUsuario: $prompt")?.text ?: uiStrings.errorConnection
        }
    }

    private suspend fun sendOpenAIMessage(prompt: String, system: String, memory: String): String {
        processingState = ProcessingState.RESPONDING
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = dataManager?.getApiKey(CloudProvider.OPENAI) ?: return@withContext "Error: No API key"
                val selModel = getSelectedCloudModel(CloudProvider.OPENAI)
                val msgs = JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", "$system\n\n$memory"))
                    conversationHistory.takeLast(3).forEach {
                        put(JSONObject().put("role", "user").put("content", it.first))
                        put(JSONObject().put("role", "assistant").put("content", it.second))
                    }
                    put(JSONObject().put("role", "user").put("content", prompt))
                }
                // GPT-5, o3 y o4 requieren max_completion_tokens en lugar de max_tokens
                val tokenParam = if (selModel.apiModelId.startsWith("gpt-5") || selModel.apiModelId.startsWith("o3") || selModel.apiModelId.startsWith("o4")) "max_completion_tokens" else "max_tokens"
                val body = JSONObject().put("model", selModel.apiModelId).put("messages", msgs).put(tokenParam, 1024).toString()
                val req = Request.Builder().url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType())).build()
                val resp = httpClient.newCall(req).execute()
                val respBody = resp.body?.string() ?: return@withContext "Error: Empty response"
                if (!resp.isSuccessful) return@withContext "Error: ${resp.code} - $respBody"
                JSONObject(respBody).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            } catch (e: Exception) { "Error: ${e.message}" }
        }
    }

    private suspend fun sendClaudeMessage(prompt: String, system: String, memory: String): String {
        processingState = ProcessingState.RESPONDING
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = dataManager?.getApiKey(CloudProvider.CLAUDE) ?: return@withContext "Error: No API key"
                val selModel = getSelectedCloudModel(CloudProvider.CLAUDE)
                val msgs = JSONArray().apply {
                    conversationHistory.takeLast(3).forEach {
                        put(JSONObject().put("role", "user").put("content", it.first))
                        put(JSONObject().put("role", "assistant").put("content", it.second))
                    }
                    put(JSONObject().put("role", "user").put("content", prompt))
                }
                val body = JSONObject().put("model", selModel.apiModelId).put("max_tokens", 1024)
                    .put("system", "$system\n\n$memory").put("messages", msgs).toString()
                val req = Request.Builder().url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", apiKey).addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json").post(body.toRequestBody("application/json".toMediaType())).build()
                val resp = httpClient.newCall(req).execute()
                val respBody = resp.body?.string() ?: return@withContext "Error: Empty response"
                if (!resp.isSuccessful) return@withContext "Error: ${resp.code} - $respBody"
                JSONObject(respBody).getJSONArray("content").getJSONObject(0).getString("text")
            } catch (e: Exception) { "Error: ${e.message}" }
        }
    }

    private suspend fun sendDeepSeekMessage(prompt: String, system: String, memory: String): String {
        processingState = ProcessingState.RESPONDING
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = dataManager?.getApiKey(CloudProvider.DEEPSEEK) ?: return@withContext "Error: No API key"
                val selModel = getSelectedCloudModel(CloudProvider.DEEPSEEK)
                val msgs = JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", "$system\n\n$memory"))
                    conversationHistory.takeLast(3).forEach {
                        put(JSONObject().put("role", "user").put("content", it.first))
                        put(JSONObject().put("role", "assistant").put("content", it.second))
                    }
                    put(JSONObject().put("role", "user").put("content", prompt))
                }
                val body = JSONObject().put("model", selModel.apiModelId).put("messages", msgs).put("max_tokens", 1024).toString()
                val req = Request.Builder().url("https://api.deepseek.com/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType())).build()
                val resp = httpClient.newCall(req).execute()
                val respBody = resp.body?.string() ?: return@withContext "Error: Empty response"
                if (!resp.isSuccessful) return@withContext "Error: ${resp.code} - $respBody"
                JSONObject(respBody).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            } catch (e: Exception) { "Error: ${e.message}" }
        }
    }

    private suspend fun sendQwenMessage(prompt: String, system: String, memory: String): String {
        processingState = ProcessingState.RESPONDING
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = dataManager?.getApiKey(CloudProvider.QWEN) ?: return@withContext "Error: No API key"
                val selModel = getSelectedCloudModel(CloudProvider.QWEN)
                val msgs = JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", "$system\n\n$memory"))
                    conversationHistory.takeLast(3).forEach {
                        put(JSONObject().put("role", "user").put("content", it.first))
                        put(JSONObject().put("role", "assistant").put("content", it.second))
                    }
                    put(JSONObject().put("role", "user").put("content", prompt))
                }
                val body = JSONObject().put("model", selModel.apiModelId).put("messages", msgs).put("max_tokens", 1024).toString()
                val req = Request.Builder().url("https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType())).build()
                val resp = httpClient.newCall(req).execute()
                val respBody = resp.body?.string() ?: return@withContext "Error: Empty response"
                if (!resp.isSuccessful) return@withContext "Error: ${resp.code} - $respBody"
                JSONObject(respBody).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            } catch (e: Exception) { "Error: ${e.message}" }
        }
    }

    private suspend fun sendKimiMessage(prompt: String, system: String, memory: String): String {
        processingState = ProcessingState.RESPONDING
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = dataManager?.getApiKey(CloudProvider.KIMI) ?: return@withContext "Error: No API key"
                val selModel = getSelectedCloudModel(CloudProvider.KIMI)
                val msgs = JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", "$system\n\n$memory"))
                    conversationHistory.takeLast(3).forEach {
                        put(JSONObject().put("role", "user").put("content", it.first))
                        put(JSONObject().put("role", "assistant").put("content", it.second))
                    }
                    put(JSONObject().put("role", "user").put("content", prompt))
                }
                val body = JSONObject().put("model", selModel.apiModelId).put("messages", msgs).put("max_tokens", 1024).toString()
                val req = Request.Builder().url("https://api.moonshot.ai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType())).build()
                val resp = httpClient.newCall(req).execute()
                val respBody = resp.body?.string() ?: return@withContext "Error: Empty response"
                if (!resp.isSuccessful) return@withContext "Error: ${resp.code} - $respBody"
                JSONObject(respBody).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            } catch (e: Exception) { "Error: ${e.message}" }
        }
    }

    // ==========================================================
    // SISTEMA DE MEMORIA
    // ==========================================================

    private suspend fun processMemoryUpdate(userMsg: String, aiResponse: String) {
        isProcessingMemory = true
        withContext(Dispatchers.IO) {
            try {
                val currentMemory = JSONObject(memoryJson)
                val currentMemorias = currentMemory.optJSONArray("memorias") ?: JSONArray()
                val langCode = uiStrings.langCode
                val memoriasTexto = StringBuilder()
                for (i in 0 until currentMemorias.length()) { 
                    memoriasTexto.append("$i: ${currentMemorias.optString(i, "")}\n") 
                }
                val analysisPrompt = buildMemoryAnalysisPrompt(langCode, memoriasTexto.toString(), userMsg, aiResponse)
                
                val resultJson = when (activeCloudProvider) {
                    CloudProvider.GEMINI -> cloudModel?.generateContent(analysisPrompt)?.text ?: "{}"
                    CloudProvider.OPENAI -> analyzeWithOpenAI(analysisPrompt)
                    CloudProvider.CLAUDE -> analyzeWithClaude(analysisPrompt)
                    CloudProvider.DEEPSEEK -> analyzeWithDeepSeek(analysisPrompt)
                    CloudProvider.QWEN -> analyzeWithQwen(analysisPrompt)
                    CloudProvider.KIMI -> analyzeWithKimi(analysisPrompt)
                    null -> "{}"
                }
                
                val cleanJson = extractJson(resultJson)
                if (cleanJson.isBlank()) return@withContext
                
                val updates = JSONObject(cleanJson)
                val eliminar = updates.optJSONArray("eliminar") ?: JSONArray()
                val indicesToRemove = mutableSetOf<Int>()
                for (i in 0 until eliminar.length()) { 
                    indicesToRemove.add(eliminar.optInt(i, -1)) 
                }
                
                val newMemorias = JSONArray()
                for (i in 0 until currentMemorias.length()) { 
                    if (i !in indicesToRemove) newMemorias.put(currentMemorias.optString(i, "")) 
                }
                
                val agregar = updates.optJSONArray("agregar") ?: JSONArray()
                for (i in 0 until agregar.length()) {
                    val nueva = agregar.optString(i, "").trim()
                    if (nueva.isNotBlank() && !isDuplicateInArray(newMemorias, nueva)) {
                        newMemorias.put(nueva)
                    }
                }
                
                val final = JSONObject().put("memorias", newMemorias)
                withContext(Dispatchers.Main) { 
                    memoryJson = final.toString()
                    dataManager?.guardarMemoria(memoryJson) 
                }
            } catch (e: Exception) { 
                e.printStackTrace() 
            }
        }
        isProcessingMemory = false
    }

    private fun buildMemoryAnalysisPrompt(langCode: String, currentMemories: String, userMsg: String, aiResponse: String): String {
        val langInstructions = when(langCode) {
            "ES" -> "Escribe las memorias en español, como frases naturales y legibles."
            "EN" -> "Write memories in English, as natural and readable sentences."
            "CN" -> "用中文写记忆，作为自然可读的句子。"
            "FR" -> "Écrivez les mémoires en français, comme des phrases naturelles et lisibles."
            "RU" -> "Пишите воспоминания на русском языке, как естественные и читаемые предложения."
            "PT" -> "Escreva as memórias em português, como frases naturais e legíveis."
            else -> "Write memories in the user's language, as natural and readable sentences."
        }
        return """Analiza esta conversación y extrae información PERMANENTE sobre el usuario.

MEMORIAS ACTUALES (índice: texto):
$currentMemories

CONVERSACIÓN:
Usuario: "$userMsg"
Asistente: "$aiResponse"

REGLAS:
1. $langInstructions
2. Solo extrae datos PERMANENTES: nombre, edad, trabajo, familia, gustos, mascotas, hobbies, etc.
3. NO incluyas: saludos, preguntas, información temporal, opiniones sobre temas externos.
4. Si el usuario CORRIGE información (ej: "no me llamo X, me llamo Y"), incluye el índice de la memoria antigua en "eliminar" y la nueva en "agregar".
5. Cada memoria debe ser una frase completa y legible (ej: "Se llama Daniel", "Tiene un gato llamado Orion", "Trabaja como ingeniero").
6. NO uses formato JSON dentro de las memorias, solo texto natural.

RESPONDE SOLO CON JSON:
{
  "agregar": ["frase legible 1", "frase legible 2"],
  "eliminar": [0, 2]
}

Si no hay nada que agregar o eliminar, responde: {"agregar": [], "eliminar": []}"""
    }

    private fun isDuplicateInArray(array: JSONArray, text: String): Boolean {
        val textLower = text.lowercase()
        for (i in 0 until array.length()) {
            val existing = array.optString(i, "").lowercase()
            if (existing.contains(textLower) || textLower.contains(existing)) return true
            val textWords = textLower.split(" ").filter { it.length > 3 }
            val existingWords = existing.split(" ").filter { it.length > 3 }
            val commonWords = textWords.intersect(existingWords.toSet())
            if (commonWords.size >= 2 && commonWords.size >= textWords.size * 0.5) return true
        }
        return false
    }

    private suspend fun analyzeWithOpenAI(prompt: String): String {
        return try {
            val apiKey = dataManager?.getApiKey(CloudProvider.OPENAI) ?: return "{}"
            val selModel = getSelectedCloudModel(CloudProvider.OPENAI)
            val msgs = JSONArray().apply { put(JSONObject().put("role", "user").put("content", prompt)) }
            // GPT-5, o3 y o4 requieren max_completion_tokens
            val tokenParam = if (selModel.apiModelId.startsWith("gpt-5") || selModel.apiModelId.startsWith("o3") || selModel.apiModelId.startsWith("o4")) "max_completion_tokens" else "max_tokens"
            val body = JSONObject().put("model", selModel.apiModelId).put("messages", msgs).put(tokenParam, 500).toString()
            val req = Request.Builder().url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return "{}"
            JSONObject(resp.body?.string() ?: "{}").getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        } catch (e: Exception) { "{}" }
    }

    private suspend fun analyzeWithClaude(prompt: String): String {
        return try {
            val apiKey = dataManager?.getApiKey(CloudProvider.CLAUDE) ?: return "{}"
            val selModel = getSelectedCloudModel(CloudProvider.CLAUDE)
            val msgs = JSONArray().apply { put(JSONObject().put("role", "user").put("content", prompt)) }
            val body = JSONObject().put("model", selModel.apiModelId).put("max_tokens", 500).put("messages", msgs).toString()
            val req = Request.Builder().url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey).addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json").post(body.toRequestBody("application/json".toMediaType())).build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return "{}"
            JSONObject(resp.body?.string() ?: "{}").getJSONArray("content").getJSONObject(0).getString("text")
        } catch (e: Exception) { "{}" }
    }

    private suspend fun analyzeWithDeepSeek(prompt: String): String {
        return try {
            val apiKey = dataManager?.getApiKey(CloudProvider.DEEPSEEK) ?: return "{}"
            val selModel = getSelectedCloudModel(CloudProvider.DEEPSEEK)
            val msgs = JSONArray().apply { put(JSONObject().put("role", "user").put("content", prompt)) }
            val body = JSONObject().put("model", selModel.apiModelId).put("messages", msgs).put("max_tokens", 500).toString()
            val req = Request.Builder().url("https://api.deepseek.com/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return "{}"
            JSONObject(resp.body?.string() ?: "{}").getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        } catch (e: Exception) { "{}" }
    }

    private suspend fun analyzeWithQwen(prompt: String): String {
        return try {
            val apiKey = dataManager?.getApiKey(CloudProvider.QWEN) ?: return "{}"
            val selModel = getSelectedCloudModel(CloudProvider.QWEN)
            val msgs = JSONArray().apply { put(JSONObject().put("role", "user").put("content", prompt)) }
            val body = JSONObject().put("model", selModel.apiModelId).put("messages", msgs).put("max_tokens", 500).toString()
            val req = Request.Builder().url("https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return "{}"
            JSONObject(resp.body?.string() ?: "{}").getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        } catch (e: Exception) { "{}" }
    }

    private suspend fun analyzeWithKimi(prompt: String): String {
        return try {
            val apiKey = dataManager?.getApiKey(CloudProvider.KIMI) ?: return "{}"
            val selModel = getSelectedCloudModel(CloudProvider.KIMI)
            val msgs = JSONArray().apply { put(JSONObject().put("role", "user").put("content", prompt)) }
            val body = JSONObject().put("model", selModel.apiModelId).put("messages", msgs).put("max_tokens", 500).toString()
            val req = Request.Builder().url("https://api.moonshot.ai/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return "{}"
            JSONObject(resp.body?.string() ?: "{}").getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        } catch (e: Exception) { "{}" }
    }

    private fun extractJson(text: String): String {
        val s = text.indexOf('{')
        val e = text.lastIndexOf('}')
        return if (s >= 0 && e > s) text.substring(s, e + 1) else ""
    }

    // ==========================================================
    // GESTIÓN DE MEMORIA MANUAL
    // ==========================================================

    fun eliminarMemoria(idx: Int) {
        try {
            val root = JSONObject(memoryJson)
            val arr = root.optJSONArray("memorias") ?: return
            val newArr = JSONArray()
            for (i in 0 until arr.length()) { 
                if (i != idx) newArr.put(arr.getString(i)) 
            }
            root.put("memorias", newArr)
            root.put("actualizado", getCurrentTimestamp())
            memoryJson = root.toString()
            dataManager?.guardarMemoria(memoryJson)
        } catch (e: Exception) { }
    }

    fun agregarMemoriaManual(texto: String) {
        if (texto.isBlank()) return
        try {
            val root = JSONObject(memoryJson)
            val arr = root.optJSONArray("memorias") ?: JSONArray()
            
            // Verificar duplicados
            val textoLower = texto.lowercase().trim()
            for (i in 0 until arr.length()) {
                val existing = arr.optString(i, "").lowercase()
                if (existing == textoLower || 
                    (existing.contains(textoLower) && textoLower.length > 10) ||
                    (textoLower.contains(existing) && existing.length > 10)) {
                    return // Ya existe algo similar
                }
            }
            
            arr.put(texto.trim())
            root.put("memorias", arr)
            root.put("actualizado", getCurrentTimestamp())
            memoryJson = root.toString()
            dataManager?.guardarMemoria(memoryJson)
        } catch (e: Exception) { }
    }

    fun editarMemoria(idx: Int, nuevoTexto: String) {
        if (nuevoTexto.isBlank()) {
            eliminarMemoria(idx)
            return
        }
        try {
            val root = JSONObject(memoryJson)
            val arr = root.optJSONArray("memorias") ?: return
            if (idx < 0 || idx >= arr.length()) return
            
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                if (i == idx) {
                    newArr.put(nuevoTexto.trim())
                } else {
                    newArr.put(arr.getString(i))
                }
            }
            root.put("memorias", newArr)
            root.put("actualizado", getCurrentTimestamp())
            memoryJson = root.toString()
            dataManager?.guardarMemoria(memoryJson)
        } catch (e: Exception) { }
    }

    private fun getCurrentTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    // Detecta comandos directos de memoria en el mensaje del usuario
    private fun detectDirectMemoryCommand(userMsg: String): Pair<String, String>? {
        val msgLower = userMsg.lowercase().trim()
        
        // Comandos para AÑADIR memoria
        val addPatterns = listOf(
            "recuerda que ", "remember that ", "记住 ", "souviens-toi que ",
            "acuérdate de que ", "acuerdate de que ", "acuérdate que ", "acuerdate que ",
            "guarda en memoria ", "guarda esto: ", "memoriza que ", "memoriza esto: ",
            "no olvides que ", "ten en cuenta que ", "apunta que ", "anota que "
        )
        
        for (pattern in addPatterns) {
            if (msgLower.startsWith(pattern)) {
                val content = userMsg.substring(pattern.length).trim()
                if (content.isNotBlank()) {
                    return Pair("add", content)
                }
            }
        }
        
        // Comandos para ELIMINAR memoria
        val removePatterns = listOf(
            "olvida que ", "forget that ", "忘记 ", "oublie que ",
            "borra de memoria ", "elimina de memoria ", "ya no ", 
            "olvida lo de ", "olvídate de ", "olvidate de "
        )
        
        for (pattern in removePatterns) {
            if (msgLower.startsWith(pattern)) {
                val content = userMsg.substring(pattern.length).trim()
                if (content.isNotBlank()) {
                    return Pair("remove", content)
                }
            }
        }
        
        // Detectar información personal directa
        val infoPatterns = listOf(
            Pair("mi nombre es ", "Se llama "),
            Pair("me llamo ", "Se llama "),
            Pair("my name is ", "User's name is "),
            Pair("tengo (\\d+) años".toRegex(), "Tiene X años"),
            Pair("trabajo como ", "Trabaja como "),
            Pair("trabajo en ", "Trabaja en "),
            Pair("soy ", "Es "),
            Pair("vivo en ", "Vive en "),
            Pair("mi mascota ", "Tiene una mascota: "),
            Pair("mi gato ", "Tiene un gato: "),
            Pair("mi perro ", "Tiene un perro: ")
        )
        
        for ((pattern, prefix) in infoPatterns) {
            when (pattern) {
                is String -> {
                    if (msgLower.startsWith(pattern)) {
                        val content = userMsg.substring(pattern.length).trim()
                        if (content.isNotBlank() && content.split(" ").size <= 5) {
                            // Capitalizar primera letra
                            val capitalizedContent = content.replaceFirstChar { it.uppercase() }
                            return Pair("add", "$prefix$capitalizedContent")
                        }
                    }
                }
                is Regex -> {
                    val match = pattern.find(msgLower)
                    if (match != null) {
                        val content = match.groupValues.getOrNull(1) ?: ""
                        if (content.isNotBlank()) {
                            return Pair("add", prefix.replace("X", content))
                        }
                    }
                }
            }
        }
        
        return null
    }

    // Procesa comando directo de memoria
    private fun processDirectMemoryCommand(command: Pair<String, String>) {
        val (action, content) = command
        when (action) {
            "add" -> agregarMemoriaManual(content)
            "remove" -> {
                // Buscar y eliminar memoria que coincida
                try {
                    val root = JSONObject(memoryJson)
                    val arr = root.optJSONArray("memorias") ?: return
                    val contentLower = content.lowercase()
                    
                    for (i in 0 until arr.length()) {
                        val mem = arr.optString(i, "").lowercase()
                        if (mem.contains(contentLower) || contentLower.contains(mem.take(20))) {
                            eliminarMemoria(i)
                            return
                        }
                    }
                } catch (e: Exception) { }
            }
        }
    }

    fun importarDatos(content: String) {
        try {
            val migrated = migrateMemoryFormat(content)
            JSONObject(migrated)
            memoryJson = migrated
            dataManager?.guardarMemoria(migrated)
        } catch (e: Exception) { }
    }

    fun borrarDatos() { 
        dataManager?.borrarMemoria()
        memoryJson = """{"memorias": [], "creado": "${getCurrentTimestamp()}", "actualizado": "${getCurrentTimestamp()}"}""" 
    }

    fun getMemoryStats(): Triple<Int, String, String> {
        return try {
            val root = JSONObject(memoryJson)
            val arr = root.optJSONArray("memorias") ?: JSONArray()
            val count = arr.length()
            val creado = root.optString("creado", "")
            val actualizado = root.optString("actualizado", "")
            Triple(count, creado, actualizado)
        } catch (e: Exception) {
            Triple(0, "", "")
        }
    }

    // ==========================================================
    // LOGOUT Y CLEANUP
    // ==========================================================

    fun logout() {
        currentGenerationJob?.cancel()
        viewModelScope.launch { localEngine?.release() }
        dataManager?.clearAllApiKeys()
        activeCloudProvider = null
        selectedModel = null
        messages.clear()
        conversationHistory.clear()
        processingState = ProcessingState.IDLE
    }

    fun getDataManager(): DataManager? = dataManager

    override fun onCleared() {
        super.onCleared()
        currentGenerationJob?.cancel()
        viewModelScope.launch { localEngine?.release() }
    }
}
