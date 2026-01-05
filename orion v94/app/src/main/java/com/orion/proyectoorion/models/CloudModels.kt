package com.orion.proyectoorion.models

// ==========================================================
// MODELOS CLOUD - DICIEMBRE 2025
// ==========================================================

data class CloudModel(
    val id: String,
    val displayName: String,
    val apiModelId: String,
    val inputPrice: Double,
    val outputPrice: Double,
    val contextWindow: Int,
    val descriptions: Map<String, String>,  // langCode -> description
    val recommended: Boolean = false
) {
    fun getDescription(langCode: String): String {
        return descriptions[langCode] ?: descriptions["EN"] ?: ""
    }
}

// ==========================================================
// GOOGLE GEMINI
// ==========================================================

val GEMINI_MODELS = listOf(
    CloudModel(
        id = "gemini-3-pro",
        displayName = "Gemini 3 Pro",
        apiModelId = "gemini-3-pro-preview",
        inputPrice = 1.25,
        outputPrice = 10.0,
        contextWindow = 1000,
        descriptions = mapOf(
            "ES" to "El más inteligente, multimodal",
            "EN" to "Most intelligent, multimodal",
            "CN" to "最智能，多模态",
            "FR" to "Le plus intelligent, multimodal",
            "RU" to "Самый умный, мультимодальный",
            "PT" to "O mais inteligente, multimodal"
        ),
        recommended = true
    ),
    CloudModel(
        id = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        apiModelId = "gemini-2.5-flash",
        inputPrice = 0.15,
        outputPrice = 0.60,
        contextWindow = 1000,
        descriptions = mapOf(
            "ES" to "Equilibrado, 1M contexto",
            "EN" to "Balanced, 1M context",
            "CN" to "均衡，100万上下文",
            "FR" to "Équilibré, contexte 1M",
            "RU" to "Сбалансированный, контекст 1M",
            "PT" to "Equilibrado, contexto 1M"
        )
    ),
    CloudModel(
        id = "gemini-2.5-flash-lite",
        displayName = "Gemini 2.5 Flash-Lite",
        apiModelId = "gemini-2.5-flash-lite",
        inputPrice = 0.075,
        outputPrice = 0.30,
        contextWindow = 1000,
        descriptions = mapOf(
            "ES" to "Ultra rápido y económico",
            "EN" to "Ultra fast and affordable",
            "CN" to "超快速且经济",
            "FR" to "Ultra rapide et économique",
            "RU" to "Ультрабыстрый и экономичный",
            "PT" to "Ultra rápido e econômico"
        )
    )
)

// ==========================================================
// OPENAI
// ==========================================================

val OPENAI_MODELS = listOf(
    CloudModel(
        id = "gpt-5",
        displayName = "GPT-5",
        apiModelId = "gpt-5",
        inputPrice = 10.0,
        outputPrice = 30.0,
        contextWindow = 256,
        descriptions = mapOf(
            "ES" to "El más inteligente de OpenAI",
            "EN" to "OpenAI's most intelligent",
            "CN" to "OpenAI最智能的模型",
            "FR" to "Le plus intelligent d'OpenAI",
            "RU" to "Самый умный от OpenAI",
            "PT" to "O mais inteligente da OpenAI"
        ),
        recommended = true
    ),
    CloudModel(
        id = "gpt-4.1",
        displayName = "GPT-4.1",
        apiModelId = "gpt-4.1",
        inputPrice = 2.0,
        outputPrice = 8.0,
        contextWindow = 1000,
        descriptions = mapOf(
            "ES" to "Equilibrado, 1M contexto",
            "EN" to "Balanced, 1M context",
            "CN" to "均衡，100万上下文",
            "FR" to "Équilibré, contexte 1M",
            "RU" to "Сбалансированный, контекст 1M",
            "PT" to "Equilibrado, contexto 1M"
        )
    ),
    CloudModel(
        id = "o4-mini",
        displayName = "o4-mini",
        apiModelId = "o4-mini",
        inputPrice = 1.10,
        outputPrice = 4.40,
        contextWindow = 200,
        descriptions = mapOf(
            "ES" to "Rápido y económico",
            "EN" to "Fast and affordable",
            "CN" to "快速且经济",
            "FR" to "Rapide et économique",
            "RU" to "Быстрый и экономичный",
            "PT" to "Rápido e econômico"
        )
    )
)

// ==========================================================
// ANTHROPIC CLAUDE
// ==========================================================

val CLAUDE_MODELS = listOf(
    CloudModel(
        id = "claude-sonnet-4.5",
        displayName = "Claude Sonnet 4.5",
        apiModelId = "claude-sonnet-4-5-20250929",
        inputPrice = 3.0,
        outputPrice = 15.0,
        contextWindow = 200,
        descriptions = mapOf(
            "ES" to "El mejor para código y agentes",
            "EN" to "Best for code and agents",
            "CN" to "代码和智能体最佳选择",
            "FR" to "Meilleur pour code et agents",
            "RU" to "Лучший для кода и агентов",
            "PT" to "O melhor para código e agentes"
        ),
        recommended = true
    ),
    CloudModel(
        id = "claude-sonnet-4",
        displayName = "Claude Sonnet 4",
        apiModelId = "claude-sonnet-4-20250514",
        inputPrice = 3.0,
        outputPrice = 15.0,
        contextWindow = 200,
        descriptions = mapOf(
            "ES" to "Equilibrado, muy fiable",
            "EN" to "Balanced, very reliable",
            "CN" to "均衡，非常可靠",
            "FR" to "Équilibré, très fiable",
            "RU" to "Сбалансированный, надёжный",
            "PT" to "Equilibrado, muito confiável"
        )
    ),
    CloudModel(
        id = "claude-haiku-4.5",
        displayName = "Claude Haiku 4.5",
        apiModelId = "claude-haiku-4-5-20251001",
        inputPrice = 1.0,
        outputPrice = 5.0,
        contextWindow = 200,
        descriptions = mapOf(
            "ES" to "Rápido y económico",
            "EN" to "Fast and affordable",
            "CN" to "快速且经济",
            "FR" to "Rapide et économique",
            "RU" to "Быстрый и экономичный",
            "PT" to "Rápido e econômico"
        )
    )
)

// ==========================================================
// DEEPSEEK
// ==========================================================

val DEEPSEEK_MODELS = listOf(
    CloudModel(
        id = "deepseek-chat",
        displayName = "DeepSeek V3",
        apiModelId = "deepseek-chat",
        inputPrice = 0.28,
        outputPrice = 0.42,
        contextWindow = 64,
        descriptions = mapOf(
            "ES" to "Chat general, muy económico",
            "EN" to "General chat, very affordable",
            "CN" to "通用聊天，非常经济",
            "FR" to "Chat général, très économique",
            "RU" to "Общий чат, очень экономичный",
            "PT" to "Chat geral, muito econômico"
        ),
        recommended = true
    ),
    CloudModel(
        id = "deepseek-reasoner",
        displayName = "DeepSeek R1",
        apiModelId = "deepseek-reasoner",
        inputPrice = 0.55,
        outputPrice = 2.19,
        contextWindow = 64,
        descriptions = mapOf(
            "ES" to "Razonamiento profundo",
            "EN" to "Deep reasoning",
            "CN" to "深度推理",
            "FR" to "Raisonnement profond",
            "RU" to "Глубокое рассуждение",
            "PT" to "Raciocínio profundo"
        )
    ),
    CloudModel(
        id = "deepseek-coder",
        displayName = "DeepSeek Coder",
        apiModelId = "deepseek-coder",
        inputPrice = 0.14,
        outputPrice = 0.28,
        contextWindow = 64,
        descriptions = mapOf(
            "ES" to "Especializado en código",
            "EN" to "Specialized in code",
            "CN" to "专注于代码",
            "FR" to "Spécialisé en code",
            "RU" to "Специализация на коде",
            "PT" to "Especializado em código"
        )
    )
)

// ==========================================================
// ALIBABA QWEN
// ==========================================================

val QWEN_MODELS = listOf(
    CloudModel(
        id = "qwen-max",
        displayName = "Qwen Max",
        apiModelId = "qwen-max",
        inputPrice = 2.40,
        outputPrice = 9.60,
        contextWindow = 32,
        descriptions = mapOf(
            "ES" to "El más potente de Alibaba",
            "EN" to "Alibaba's most powerful",
            "CN" to "阿里巴巴最强大的模型",
            "FR" to "Le plus puissant d'Alibaba",
            "RU" to "Самый мощный от Alibaba",
            "PT" to "O mais potente da Alibaba"
        ),
        recommended = true
    ),
    CloudModel(
        id = "qwen-plus",
        displayName = "Qwen Plus",
        apiModelId = "qwen-plus",
        inputPrice = 0.80,
        outputPrice = 2.0,
        contextWindow = 131,
        descriptions = mapOf(
            "ES" to "Equilibrado, gran contexto",
            "EN" to "Balanced, large context",
            "CN" to "均衡，大上下文",
            "FR" to "Équilibré, grand contexte",
            "RU" to "Сбалансированный, большой контекст",
            "PT" to "Equilibrado, grande contexto"
        )
    ),
    CloudModel(
        id = "qwen-turbo",
        displayName = "Qwen Turbo",
        apiModelId = "qwen-turbo",
        inputPrice = 0.30,
        outputPrice = 0.60,
        contextWindow = 1000,
        descriptions = mapOf(
            "ES" to "Ultra rápido",
            "EN" to "Ultra fast",
            "CN" to "超快速",
            "FR" to "Ultra rapide",
            "RU" to "Ультрабыстрый",
            "PT" to "Ultra rápido"
        )
    ),
    CloudModel(
        id = "qwen-long",
        displayName = "Qwen Long",
        apiModelId = "qwen-long",
        inputPrice = 0.50,
        outputPrice = 2.0,
        contextWindow = 10000,
        descriptions = mapOf(
            "ES" to "Contexto masivo 10M",
            "EN" to "Massive 10M context",
            "CN" to "超大1000万上下文",
            "FR" to "Contexte massif 10M",
            "RU" to "Огромный контекст 10M",
            "PT" to "Contexto massivo 10M"
        )
    )
)

// ==========================================================
// MOONSHOT KIMI
// ==========================================================

val KIMI_MODELS = listOf(
    CloudModel(
        id = "kimi-k2",
        displayName = "Kimi K2",
        apiModelId = "kimi-k2-0711-preview",
        inputPrice = 0.60,
        outputPrice = 2.50,
        contextWindow = 128,
        descriptions = mapOf(
            "ES" to "1T parámetros, muy potente",
            "EN" to "1T parameters, very powerful",
            "CN" to "1万亿参数，非常强大",
            "FR" to "1T paramètres, très puissant",
            "RU" to "1T параметров, очень мощный",
            "PT" to "1T parâmetros, muito potente"
        ),
        recommended = true
    ),
    CloudModel(
        id = "moonshot-v1-8k",
        displayName = "Moonshot v1 8K",
        apiModelId = "moonshot-v1-8k",
        inputPrice = 0.20,
        outputPrice = 2.0,
        contextWindow = 8,
        descriptions = mapOf(
            "ES" to "Económico, contexto corto",
            "EN" to "Affordable, short context",
            "CN" to "经济，短上下文",
            "FR" to "Économique, contexte court",
            "RU" to "Экономичный, короткий контекст",
            "PT" to "Econômico, contexto curto"
        )
    ),
    CloudModel(
        id = "moonshot-v1-32k",
        displayName = "Moonshot v1 32K",
        apiModelId = "moonshot-v1-32k",
        inputPrice = 1.0,
        outputPrice = 3.0,
        contextWindow = 32,
        descriptions = mapOf(
            "ES" to "Contexto medio",
            "EN" to "Medium context",
            "CN" to "中等上下文",
            "FR" to "Contexte moyen",
            "RU" to "Средний контекст",
            "PT" to "Contexto médio"
        )
    ),
    CloudModel(
        id = "moonshot-v1-128k",
        displayName = "Moonshot v1 128K",
        apiModelId = "moonshot-v1-128k",
        inputPrice = 2.0,
        outputPrice = 5.0,
        contextWindow = 128,
        descriptions = mapOf(
            "ES" to "Contexto largo",
            "EN" to "Long context",
            "CN" to "长上下文",
            "FR" to "Contexte long",
            "RU" to "Длинный контекст",
            "PT" to "Contexto longo"
        )
    )
)
