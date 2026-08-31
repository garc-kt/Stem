package com.veggiebit.sprout.features.enhancement.data.models

/**
 * AI / Rule engine selection mode.
 */
enum class EngineMode(
    val id: String,
    val title: String,
    val description: String,
    val isCloud: Boolean
) {
    LOCAL_RULES(
        id = "local_rules",
        title = "On-Device Rules (Instant)",
        description = "100% offline, 0ms latency in local RAM. Zero network.",
        isCloud = false
    ),
    OLLAMA_AI(
        id = "ollama_ai",
        title = "Ollama Local AI (PC / LAN)",
        description = "Connects over Wi-Fi to Ollama on your PC (Llama 3.3, Mistral, Gemma 2).",
        isCloud = false
    ),
    GEMINI_AI(
        id = "gemini_ai",
        title = "Google Gemini",
        description = "Ultra-fast text enhancement using Google Gemini 3.7 Flash and newer.",
        isCloud = true
    ),
    OPENAI_COMPATIBLE(
        id = "openai_compatible",
        title = "OpenAI / OpenRouter / DeepSeek",
        description = "GPT-5, DeepSeek, Groq, or any OpenAI-compatible API endpoint.",
        isCloud = true
    ),
    CLAUDE_AI(
        id = "claude_ai",
        title = "Anthropic Claude",
        description = "High-precision writing enhancement using Claude 4.5 Sonnet and newer.",
        isCloud = true
    );

    companion object {
        fun fromId(id: String): EngineMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LOCAL_RULES
        }
    }
}

