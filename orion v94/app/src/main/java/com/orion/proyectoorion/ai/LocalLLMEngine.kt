package com.orion.proyectoorion.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interface for LLM generation callbacks
 */
interface LLMCallback {
    fun onToken(token: String)
    fun onComplete()
    fun onError(error: String)
}

/**
 * Local LLM Engine using llama.cpp via JNI
 */
class LocalLLMEngine(private val context: Context) {

    companion object {
        private const val TAG = "LocalLLMEngine"

        init {
            try {
                System.loadLibrary("llama-android")
                Log.i(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }

    private var isLoaded = false

    // Native methods - these match the JNI functions in llama-jni.cpp
    @Suppress("KotlinJniMissingFunction")
    private external fun initBackend()

    @Suppress("KotlinJniMissingFunction")
    private external fun loadModelNative(modelPath: String, contextSize: Int, nThreads: Int): Boolean

    @Suppress("KotlinJniMissingFunction")
    private external fun generateNative(prompt: String, maxTokens: Int, callback: LLMCallback): String

    @Suppress("KotlinJniMissingFunction")
    private external fun stopGenerationNative()

    @Suppress("KotlinJniMissingFunction")
    private external fun freeModelNative()

    @Suppress("KotlinJniMissingFunction")
    private external fun isModelLoaded(): Boolean

    @Suppress("KotlinJniMissingFunction")
    private external fun getVocabSize(): Int

    @Suppress("KotlinJniMissingFunction")
    private external fun getContextSize(): Int

    @Suppress("KotlinJniMissingFunction")
    private external fun cleanupBackend()

    @Suppress("KotlinJniMissingFunction")
    private external fun getSystemInfoNative(): String

    init {
        try {
            initBackend()
            Log.i(TAG, "Backend initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize backend: ${e.message}")
        }
    }

    /**
     * Load a GGUF model from the specified path
     */
    suspend fun loadModel(
        modelPath: String,
        contextSize: Int = 2048,
        threads: Int = 4
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Loading model: $modelPath")

            try {
                val sysInfo = getSystemInfoNative()
                Log.i(TAG, "System info: $sysInfo")
            } catch (e: Exception) {
                Log.w(TAG, "Could not get system info: ${e.message}")
            }

            val success = loadModelNative(modelPath, contextSize, threads)
            isLoaded = success

            if (success) {
                Log.i(TAG, "Model loaded successfully")
                try {
                    Log.i(TAG, "Vocab size: ${getVocabSize()}, Context size: ${getContextSize()}")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get model info: ${e.message}")
                }
            } else {
                Log.e(TAG, "Failed to load model")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Generate text from the given prompt with streaming callback
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = 512,
        callback: LLMCallback
    ) = withContext(Dispatchers.IO) {
        if (!isLoaded) {
            callback.onError("Model not loaded")
            return@withContext
        }

        try {
            Log.d(TAG, "Starting generation, prompt length: ${prompt.length}")
            generateNative(prompt, maxTokens, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Generation error: ${e.message}")
            e.printStackTrace()
            callback.onError(e.message ?: "Unknown error")
        }
    }

    /**
     * Stop the current generation
     */
    fun stopGeneration() {
        try {
            stopGenerationNative()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping generation: ${e.message}")
        }
    }

    /**
     * Release all resources
     */
    suspend fun release() = withContext(Dispatchers.IO) {
        try {
            if (isLoaded) {
                freeModelNative()
                isLoaded = false
                Log.i(TAG, "Model released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing model: ${e.message}")
        }
    }

    /**
     * Check if a model is currently loaded
     */
    fun isReady(): Boolean {
        return try {
            isLoaded && isModelLoaded()
        } catch (e: Exception) {
            false
        }
    }
}
