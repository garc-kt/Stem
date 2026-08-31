package com.veggiebit.sprout.features.enhancement.data.models

/**
 * Which dictionary [com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine]
 * uses. AI engines ignore this entirely — they infer language from the input text natively.
 */
enum class LanguagePreference(val id: String, val label: String) {
    AUTO(id = "auto", label = "Auto-detect"),
    ENGLISH(id = "english", label = "English"),
    SPANISH(id = "spanish", label = "Español");

    companion object {
        fun fromId(id: String): LanguagePreference =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AUTO
    }
}
