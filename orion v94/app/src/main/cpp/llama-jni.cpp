// llama-jni.cpp for llama.cpp b3600
// Place in: app/src/main/cpp/llama-jni.cpp

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <thread>
#include <atomic>
#include <cmath>

#include "llama.h"

#define LOG_TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;
static std::atomic<bool> g_is_generating(false);

extern "C" {

JNIEXPORT void JNICALL
Java_com_orion_proyectoorion_ai_LocalLLMEngine_initBackend(JNIEnv* env, jobject thiz) {
    LOGI("Initializing llama backend");
    llama_backend_init();
}

JNIEXPORT jstring JNICALL
Java_com_orion_proyectoorion_ai_LocalLLMEngine_getSystemInfoNative(JNIEnv* env, jobject thiz) {
    const char* info = llama_print_system_info();
    return env->NewStringUTF(info ? info : "System info unavailable");
}

JNIEXPORT jboolean JNICALL
Java_com_orion_proyectoorion_ai_LocalLLMEngine_loadModelNative(
        JNIEnv* env, jobject thiz,
        jstring modelPath, jint contextSize, jint nThreads) {

    if (g_ctx != nullptr) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model != nullptr) {
        llama_free_model(g_model);
        g_model = nullptr;
    }

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model: %s", path);

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;

    g_model = llama_load_model_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!g_model) {
        LOGE("Failed to load model");
        return JNI_FALSE;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextSize > 0 ? contextSize : 2048;
    ctx_params.n_threads = nThreads > 0 ? nThreads : std::thread::hardware_concurrency();
    ctx_params.n_threads_batch = ctx_params.n_threads;

    LOGI("Context: n_ctx=%d, threads=%d", ctx_params.n_ctx, ctx_params.n_threads);

    g_ctx = llama_new_context_with_model(g_model, ctx_params);
    if (!g_ctx) {
        LOGE("Failed to create context");
        llama_free_model(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    LOGI("Model loaded successfully");
    return JNI_TRUE;
}

// Simple top-p sampling
static llama_token sample_token(llama_context* ctx, llama_model* model, float temp, float top_p) {
    float* logits = llama_get_logits_ith(ctx, -1);
    int n_vocab = llama_n_vocab(model);

    // Apply temperature
    if (temp > 0) {
        for (int i = 0; i < n_vocab; i++) {
            logits[i] /= temp;
        }
    }

    // Softmax
    float max_logit = logits[0];
    for (int i = 1; i < n_vocab; i++) {
        if (logits[i] > max_logit) max_logit = logits[i];
    }

    std::vector<float> probs(n_vocab);
    float sum = 0.0f;
    for (int i = 0; i < n_vocab; i++) {
        probs[i] = expf(logits[i] - max_logit);
        sum += probs[i];
    }
    for (int i = 0; i < n_vocab; i++) {
        probs[i] /= sum;
    }

    // Sort for top-p
    std::vector<std::pair<float, llama_token>> prob_idx(n_vocab);
    for (int i = 0; i < n_vocab; i++) {
        prob_idx[i] = {probs[i], i};
    }
    std::sort(prob_idx.begin(), prob_idx.end(), [](auto& a, auto& b) { return a.first > b.first; });

    // Top-p filtering
    float cumsum = 0.0f;
    int last_idx = 0;
    for (int i = 0; i < n_vocab; i++) {
        cumsum += prob_idx[i].first;
        last_idx = i;
        if (cumsum >= top_p) break;
    }

    // Sample from filtered distribution
    float r = (float)rand() / RAND_MAX * cumsum;
    cumsum = 0.0f;
    for (int i = 0; i <= last_idx; i++) {
        cumsum += prob_idx[i].first;
        if (r <= cumsum) {
            return prob_idx[i].second;
        }
    }

    return prob_idx[0].second;
}

JNIEXPORT jstring JNICALL
Java_com_orion_proyectoorion_ai_LocalLLMEngine_generateNative(
        JNIEnv* env, jobject thiz,
        jstring prompt, jint maxTokens, jobject callback) {

    if (!g_model || !g_ctx) {
        LOGE("Model not loaded");
        return env->NewStringUTF("");
    }

    g_is_generating.store(true);

    const char* prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    std::string prompt_str(prompt_cstr);
    env->ReleaseStringUTFChars(prompt, prompt_cstr);

    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onCompleteMethod = env->GetMethodID(callbackClass, "onComplete", "()V");
    jmethodID onErrorMethod = env->GetMethodID(callbackClass, "onError", "(Ljava/lang/String;)V");

    // Tokenize
    std::vector<llama_token> tokens(prompt_str.length() + 256);
    int n_tokens = llama_tokenize(g_model, prompt_str.c_str(), prompt_str.length(),
                                   tokens.data(), tokens.size(), true, true);
    if (n_tokens < 0) {
        LOGE("Tokenization failed");
        env->CallVoidMethod(callback, onErrorMethod, env->NewStringUTF("Tokenization failed"));
        g_is_generating.store(false);
        return env->NewStringUTF("");
    }
    tokens.resize(n_tokens);
    LOGI("Tokenized into %d tokens", n_tokens);

    // Clear KV cache
    llama_kv_cache_clear(g_ctx);

    // Create batch and process prompt
    llama_batch batch = llama_batch_init(512, 0, 1);
    
    // Add prompt tokens to batch
    for (int i = 0; i < n_tokens; i++) {
        batch.token[i] = tokens[i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == n_tokens - 1); // Only get logits for last token
    }
    batch.n_tokens = n_tokens;

    if (llama_decode(g_ctx, batch) != 0) {
        LOGE("Decode failed");
        llama_batch_free(batch);
        env->CallVoidMethod(callback, onErrorMethod, env->NewStringUTF("Decode failed"));
        g_is_generating.store(false);
        return env->NewStringUTF("");
    }

    // Generate
    std::string result;
    int n_max = maxTokens > 0 ? maxTokens : 512;
    int n_cur = n_tokens;

    for (int i = 0; i < n_max && g_is_generating.load(); i++) {
        // Sample next token
        llama_token new_token = sample_token(g_ctx, g_model, 0.7f, 0.9f);

        // Check for EOS
        if (llama_token_is_eog(g_model, new_token)) {
            LOGI("EOS token received");
            break;
        }

        // Convert to text
        char buf[256];
        int n = llama_token_to_piece(g_model, new_token, buf, sizeof(buf), 0, true);
        if (n > 0) {
            std::string piece(buf, n);
            result += piece;

            jstring tokenStr = env->NewStringUTF(piece.c_str());
            env->CallVoidMethod(callback, onTokenMethod, tokenStr);
            env->DeleteLocalRef(tokenStr);
        }

        // Prepare next batch
        batch.n_tokens = 0;
        batch.token[0] = new_token;
        batch.pos[0] = n_cur;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = true;
        batch.n_tokens = 1;

        if (llama_decode(g_ctx, batch) != 0) {
            LOGE("Decode failed during generation");
            break;
        }
        n_cur++;
    }

    llama_batch_free(batch);
    env->CallVoidMethod(callback, onCompleteMethod);
    g_is_generating.store(false);
    
    LOGI("Generation complete, %d tokens", n_cur - n_tokens);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_orion_proyectoorion_ai_LocalLLMEngine_stopGenerationNative(JNIEnv* env, jobject thiz) {
    g_is_generating.store(false);
}

JNIEXPORT void JNICALL
Java_com_orion_proyectoorion_ai_LocalLLMEngine_freeModelNative(JNIEnv* env, jobject thiz) {
    g_is_generating.store(false);
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }
}

JNIEXPORT jboolean JNICALL
Java_com_orion_proyectoorion_ai_LocalLLMEngine_isModelLoaded(JNIEnv* env, jobject thiz) {
    return (g_model && g_ctx) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_orion_proyectoorion_ai_LocalLLMEngine_getVocabSize(JNIEnv* env, jobject thiz) {
    return g_model ? llama_n_vocab(g_model) : 0;
}

JNIEXPORT jint JNICALL
Java_com_orion_proyectoorion_ai_LocalLLMEngine_getContextSize(JNIEnv* env, jobject thiz) {
    return g_ctx ? llama_n_ctx(g_ctx) : 0;
}

JNIEXPORT void JNICALL
Java_com_orion_proyectoorion_ai_LocalLLMEngine_cleanupBackend(JNIEnv* env, jobject thiz) {
    llama_backend_free();
}

} // extern "C"
