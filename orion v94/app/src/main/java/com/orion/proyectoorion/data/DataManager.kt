package com.orion.proyectoorion.data

import android.content.Context
import com.orion.proyectoorion.models.AvailableModel
import com.orion.proyectoorion.models.CloudModel
import com.orion.proyectoorion.models.CloudProvider
import java.io.File

// ==========================================================
// DATA MANAGER - PERSISTENCIA
// ==========================================================

class DataManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("orion_secure_prefs", Context.MODE_PRIVATE)
    private val modelsDir = File(context.filesDir, "models")

    init {
        if (!modelsDir.exists()) modelsDir.mkdirs()
    }

    fun getModelsDir(): File = modelsDir

    // ==========================================================
    // MEMORIA (JSON simple)
    // ==========================================================

    fun leerMemoria(): String = try {
        context.openFileInput("memoria_cerebro.json").bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        """{"memorias": []}"""
    }

    fun guardarMemoria(json: String) {
        context.openFileOutput("memoria_cerebro.json", Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    fun borrarMemoria() {
        context.deleteFile("memoria_cerebro.json")
    }

    // ==========================================================
    // IDIOMA
    // ==========================================================

    fun saveLang(code: String) = prefs.edit().putString("lang", code).apply()
    
    fun getLang(): String? = prefs.getString("lang", null)
    
    fun isLanguageConfigured(): Boolean = prefs.contains("lang")

    // ==========================================================
    // MODO (Local/Cloud)
    // ==========================================================

    fun saveMode(isLocal: Boolean) = prefs.edit().putBoolean("is_local", isLocal).apply()
    
    fun isLocal(): Boolean = prefs.getBoolean("is_local", false)

    // ==========================================================
    // API KEYS
    // ==========================================================

    fun saveApiKey(provider: CloudProvider, key: String) =
        prefs.edit().putString("api_key_${provider.name}", key).apply()

    fun getApiKey(provider: CloudProvider): String? = 
        prefs.getString("api_key_${provider.name}", null)

    fun clearApiKey(provider: CloudProvider) = 
        prefs.edit().remove("api_key_${provider.name}").apply()

    fun clearAllApiKeys() {
        val e = prefs.edit()
        CloudProvider.entries.forEach { e.remove("api_key_${it.name}") }
        e.apply()
    }

    fun hasApiKey(provider: CloudProvider): Boolean = 
        !getApiKey(provider).isNullOrBlank()

    // ==========================================================
    // PROVEEDOR ACTIVO
    // ==========================================================

    fun saveActiveProvider(provider: CloudProvider) =
        prefs.edit().putString("active_provider", provider.name).apply()

    fun getActiveProvider(): CloudProvider? = prefs.getString("active_provider", null)?.let {
        try { CloudProvider.valueOf(it) } catch (e: Exception) { null }
    }

    // ==========================================================
    // MODELO CLOUD SELECCIONADO POR PROVEEDOR
    // ==========================================================

    fun saveSelectedCloudModel(provider: CloudProvider, modelId: String) =
        prefs.edit().putString("cloud_model_${provider.name}", modelId).apply()

    fun getSelectedCloudModel(provider: CloudProvider): String? =
        prefs.getString("cloud_model_${provider.name}", null)

    // ==========================================================
    // MODELO LOCAL SELECCIONADO
    // ==========================================================

    fun saveSelectedModel(modelId: String) = 
        prefs.edit().putString("selected_model", modelId).apply()

    fun getSelectedModel(): String? = 
        prefs.getString("selected_model", null)

    // ==========================================================
    // GESTIÓN DE MODELOS DESCARGADOS
    // ==========================================================

    /**
     * Verifica si un modelo está descargado usando su fileName
     */
    fun isModelDownloaded(model: AvailableModel): Boolean = 
        File(modelsDir, model.fileName).exists()

    /**
     * Obtiene la ruta completa del modelo
     */
    fun getModelPath(model: AvailableModel): String = 
        File(modelsDir, model.fileName).absolutePath

    /**
     * Elimina un modelo descargado
     */
    fun deleteModel(model: AvailableModel) {
        File(modelsDir, model.fileName).delete()
    }

    /**
     * Obtiene el tamaño total de modelos descargados en bytes
     */
    fun getDownloadedModelsSize(): Long {
        return modelsDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}
