package com.orion.proyectoorion.models

// ==========================================================
// MODELOS LOCALES DISPONIBLES
// ==========================================================

data class AvailableModel(
    val id: String,
    val name: String,
    val descriptions: Map<String, String>,  // langCode -> description
    val sizeGB: Float,
    val downloadUrl: String,
    val fileName: String,
    val chatTemplate: ChatTemplate = ChatTemplate.LLAMA3,
    val recommended: Boolean = false,
    val stopTokens: List<String> = emptyList()
) {
    fun getDescription(langCode: String): String {
        return descriptions[langCode] ?: descriptions["EN"] ?: ""
    }
}

enum class ChatTemplate {
    CHATML,
    LLAMA3,
    PHI3,
    GEMMA3,
    NEMOTRON
}

// ==========================================================
// MODELOS LOCALES - LISTA COMPLETA
// ==========================================================

val AVAILABLE_MODELS = listOf(
    AvailableModel(
        id = "nemotron-nano-4b",
        name = "🚀 Nemotron Nano 4B",
        descriptions = mapOf(
            "ES" to "NVIDIA. El más potente local",
            "EN" to "NVIDIA. Most powerful local",
            "CN" to "NVIDIA。最强大的本地模型",
            "FR" to "NVIDIA. Le plus puissant en local",
            "RU" to "NVIDIA. Самый мощный локальный",
            "PT" to "NVIDIA. O mais potente local"
        ),
        sizeGB = 2.72f,
        downloadUrl = "https://huggingface.co/bartowski/Nemotron-Mini-4B-Instruct-GGUF/resolve/main/Nemotron-Mini-4B-Instruct-Q4_K_M.gguf",
        fileName = "Nemotron-Mini-4B-Instruct-Q4_K_M.gguf",
        chatTemplate = ChatTemplate.NEMOTRON,
        recommended = true,
        stopTokens = listOf("<|eot_id|>", "<extra_id_1>")
    ),

    AvailableModel(
        id = "llama-3.2-3b",
        name = "🦙 Llama 3.2 3B",
        descriptions = mapOf(
            "ES" to "Equilibrio velocidad/inteligencia",
            "EN" to "Speed/intelligence balance",
            "CN" to "速度与智能的平衡",
            "FR" to "Équilibre vitesse/intelligence",
            "RU" to "Баланс скорости и интеллекта",
            "PT" to "Equilíbrio velocidade/inteligência"
        ),
        sizeGB = 2.02f,
        downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
        fileName = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
        chatTemplate = ChatTemplate.LLAMA3,
        recommended = true,
        stopTokens = listOf("<|eot_id|>", "<|end_of_text|>")
    ),

    AvailableModel(
        id = "phi-3.5-mini",
        name = "🔬 Phi-3.5 Mini",
        descriptions = mapOf(
            "ES" to "Microsoft. Ideal para razonamiento",
            "EN" to "Microsoft. Ideal for reasoning",
            "CN" to "微软。推理能力出色",
            "FR" to "Microsoft. Idéal pour le raisonnement",
            "RU" to "Microsoft. Идеален для рассуждений",
            "PT" to "Microsoft. Ideal para raciocínio"
        ),
        sizeGB = 2.39f,
        downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
        fileName = "Phi-3.5-mini-instruct-Q4_K_M.gguf",
        chatTemplate = ChatTemplate.PHI3,
        recommended = true,
        stopTokens = listOf("<|end|>", "<|endoftext|>", "<|assistant|>")
    ),

    AvailableModel(
        id = "hermes-3-3b",
        name = "⚡ Hermes 3 3B",
        descriptions = mapOf(
            "ES" to "NousResearch. Conversacional",
            "EN" to "NousResearch. Conversational",
            "CN" to "NousResearch。对话能力强",
            "FR" to "NousResearch. Conversationnel",
            "RU" to "NousResearch. Разговорный",
            "PT" to "NousResearch. Conversacional"
        ),
        sizeGB = 2.02f,
        downloadUrl = "https://huggingface.co/bartowski/Hermes-3-Llama-3.2-3B-GGUF/resolve/main/Hermes-3-Llama-3.2-3B-Q4_K_M.gguf",
        fileName = "Hermes-3-Llama-3.2-3B-Q4_K_M.gguf",
        chatTemplate = ChatTemplate.CHATML,
        recommended = true,
        stopTokens = listOf("<|im_end|>", "<|endoftext|>")
    )
)

// ==========================================================
// FUNCIONES HELPER
// ==========================================================

fun getLocalModelById(id: String): AvailableModel? = AVAILABLE_MODELS.find { it.id == id }

fun getLocalModelByFileName(fileName: String): AvailableModel? = AVAILABLE_MODELS.find { it.fileName == fileName }
