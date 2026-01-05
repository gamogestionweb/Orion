package com.orion.proyectoorion.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * LlamaAndroid - Kotlin wrapper for llama.cpp native library
 */
class LlamaAndroid private constructor() {

    companion object {
        private const val TAG = "LlamaAndroid"
        
        @Volatile
        private var instance: LlamaAndroid? = null
        private var backendInitialized = false
        private var libraryLoaded = false

        fun getInstance(): LlamaAndroid {
            return instance ?: synchronized(this) {
                instance ?: LlamaAndroid().also {
                    instance = it
                    if (libraryLoaded && !backendInitialized) {
                        try {
                            native_init_backend()
                            backendInitialized = true
                            Log.i(TAG, "Backend initialized")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to initialize backend: ${e.message}")
                        }
                    }
                }
            }
        }

        init {
            try {
                System.loadLibrary("llama-android")
                libraryLoaded = true
                Log.i(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
                libraryLoaded = false
            }
        }

        // Native methods
        @JvmStatic private external fun native_init_backend()
        @JvmStatic private external fun native_free_backend()
        @JvmStatic private external fun native_load_model(path: String): Boolean
        @JvmStatic private external fun native_free_model()
        @JvmStatic private external fun native_is_loaded(): Boolean
        @JvmStatic private external fun native_start_completion(prompt: String): Boolean
        @JvmStatic private external fun native_get_next_token(): String
        @JvmStatic private external fun native_is_generating(): Boolean
        @JvmStatic private external fun native_stop_generation()
        @JvmStatic private external fun native_clear_context()
        @JvmStatic private external fun native_get_context_size(): Int
        @JvmStatic private external fun native_bench(): String
    }

    suspend fun loadModel(pathToModel: String): Boolean = withContext(Dispatchers.IO) {
        if (!libraryLoaded) {
            Log.e(TAG, "Native library not loaded")
            return@withContext false
        }
        
        Log.i(TAG, "Loading model: $pathToModel")
        
        try {
            val result = native_load_model(pathToModel)
            Log.i(TAG, "Model load result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading model: ${e.message}")
            false
        }
    }

    fun isLoaded(): Boolean {
        return try {
            libraryLoaded && native_is_loaded()
        } catch (e: Exception) {
            false
        }
    }

    fun unload() {
        try {
            if (libraryLoaded) {
                native_stop_generation()
                native_free_model()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading: ${e.message}")
        }
    }

    fun send(message: String): Flow<String> = flow {
        if (!isLoaded()) {
            emit("[Error: Model not loaded]")
            return@flow
        }

        if (!native_start_completion(message)) {
            emit("[Error: Failed to start generation]")
            return@flow
        }

        var tokenCount = 0
        val maxTokens = 2048
        var emptyCount = 0

        while (native_is_generating() && tokenCount < maxTokens) {
            val token = native_get_next_token()
            
            if (token.isEmpty()) {
                emptyCount++
                if (emptyCount >= 5) break
                continue
            }
            
            emptyCount = 0
            emit(token)
            tokenCount++
        }

        native_stop_generation()
    }.flowOn(Dispatchers.IO)

    fun stopGeneration() {
        try {
            if (libraryLoaded) native_stop_generation()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping: ${e.message}")
        }
    }

    fun clearContext() {
        try {
            if (libraryLoaded) native_clear_context()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing: ${e.message}")
        }
    }

    fun bench(): String {
        return try {
            if (libraryLoaded) native_bench() else "Not loaded"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun destroy() {
        try {
            if (libraryLoaded) {
                unload()
                native_free_backend()
                backendInitialized = false
            }
            instance = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying: ${e.message}")
        }
    }
}
