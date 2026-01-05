package com.orion.proyectoorion.models

import androidx.compose.ui.graphics.Color
import com.orion.proyectoorion.ui.OrionBlue
import com.orion.proyectoorion.ui.OrionGreen

// ==========================================================
// COLORES DE PROVEEDORES
// ==========================================================

val OrionClaude = Color(0xFFDA7756)
val OrionChatGPT = Color(0xFF10A37F)
val OrionGemini = Color(0xFF4285F4)
val OrionDeepSeek = Color(0xFF5B6EE1)
val OrionQwen = Color(0xFFFF6A00)
val OrionKimi = Color(0xFF6366F1)

// ==========================================================
// PROVEEDORES CLOUD
// ==========================================================

enum class CloudProvider(
    val displayName: String,
    val iconResName: String,
    val color: Color,
    val apiUrl: String,
    val keyUrl: String,
    val country: String,
    val models: List<CloudModel>
) {
    GEMINI(
        displayName = "Google Gemini",
        iconResName = "ic_provider_gemini",
        color = OrionGemini,
        apiUrl = "https://generativelanguage.googleapis.com/v1beta/models",
        keyUrl = "https://aistudio.google.com/app/apikey",
        country = "🇺🇸",
        models = GEMINI_MODELS
    ),

    DEEPSEEK(
        displayName = "DeepSeek",
        iconResName = "ic_provider_deepseek",
        color = OrionDeepSeek,
        apiUrl = "https://api.deepseek.com/chat/completions",
        keyUrl = "https://platform.deepseek.com/api_keys",
        country = "🇨🇳",
        models = DEEPSEEK_MODELS
    ),

    OPENAI(
        displayName = "OpenAI ChatGPT",
        iconResName = "ic_provider_openai",
        color = OrionChatGPT,
        apiUrl = "https://api.openai.com/v1/chat/completions",
        keyUrl = "https://platform.openai.com/api-keys",
        country = "🇺🇸",
        models = OPENAI_MODELS
    ),

    QWEN(
        displayName = "Alibaba Qwen",
        iconResName = "ic_provider_qwen",
        color = OrionQwen,
        apiUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions",
        keyUrl = "https://dashscope.console.aliyun.com/apiKey",
        country = "🇨🇳",
        models = QWEN_MODELS
    ),

    CLAUDE(
        displayName = "Anthropic Claude",
        iconResName = "ic_provider_claude",
        color = OrionClaude,
        apiUrl = "https://api.anthropic.com/v1/messages",
        keyUrl = "https://console.anthropic.com/settings/keys",
        country = "🇺🇸",
        models = CLAUDE_MODELS
    ),

    KIMI(
        displayName = "Moonshot Kimi",
        iconResName = "ic_provider_kimi",
        color = OrionKimi,
        apiUrl = "https://api.moonshot.ai/v1/chat/completions",
        keyUrl = "https://platform.moonshot.ai/",
        country = "🇨🇳",
        models = KIMI_MODELS
    );

    fun getDescription(langCode: String): String = when (this) {
        GEMINI -> if (langCode == "ES") "Google Gemini - Multimodal avanzado" else "Google Gemini - Advanced multimodal"
        OPENAI -> if (langCode == "ES") "OpenAI ChatGPT - El pionero de la IA" else "OpenAI ChatGPT - The AI pioneer"
        CLAUDE -> if (langCode == "ES") "Anthropic Claude - Razonamiento superior" else "Anthropic Claude - Superior reasoning"
        DEEPSEEK -> if (langCode == "ES") "DeepSeek - El gigante open-source chino" else "DeepSeek - Chinese open-source giant"
        QWEN -> if (langCode == "ES") "Alibaba Qwen - IA de Alibaba Cloud" else "Alibaba Qwen - Alibaba Cloud AI"
        KIMI -> if (langCode == "ES") "Moonshot Kimi - 1T parámetros" else "Moonshot Kimi - 1T parameters"
    }

    fun getDefaultModel(): CloudModel = models.firstOrNull { it.recommended } ?: models.first()
}
