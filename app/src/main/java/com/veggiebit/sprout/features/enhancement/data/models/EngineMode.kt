package com.veggiebit.sprout.features.enhancement.data.models

/**
 * AI / Rule engine selection mode.
 */
enum class EngineMode(
    val id: String,
    val title: String,
    val description: String
) {
    LOCAL_RULES(
        id = "local_rules",
        title = "On-Device Rules (Instant)",
        description = "100% offline, zero latency, dictionary & grammar rules in local RAM."
    ),
    OLLAMA_AI(
        id = "ollama_ai",
        title = "Ollama Local AI (PC / LAN)",
        description = "Connects over Wi-Fi/LAN to Ollama running on your PC (Llama 3.2, Mistral, Gemma 2, etc.)."
    );

    companion object {
        fun fromId(id: String): EngineMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LOCAL_RULES
        }
    }
}
