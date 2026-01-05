package com.orion.proyectoorion.data

// ==========================================================
// IDIOMAS SOPORTADOS
// ==========================================================

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String
)

val SupportedLanguages = listOf(
    Language("es", "Spanish", "Español", "🇪🇸"),
    Language("en", "English", "English", "🇬🇧")
)

fun getLanguageByCode(code: String): Language {
    return SupportedLanguages.find { it.code == code } ?: SupportedLanguages[0]
}
