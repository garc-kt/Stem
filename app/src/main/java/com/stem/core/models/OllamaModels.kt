package com.stem.core.models

import kotlinx.serialization.Serializable



@Serializable
data class OllamaOptions(
    val temperature: Float? = null
)

@Serializable
data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val system: String? = null,
    val stream: Boolean = false,
    val options: OllamaOptions? = null
)

@Serializable
data class OllamaGenerateResponse(
    val model: String? = null,
    val response: String? = null,
    val done: Boolean = false,
    val error: String? = null
)

@Serializable
data class OllamaTagsResponse(
    val models: List<OllamaModelInfo> = emptyList()
)

@Serializable
data class OllamaModelInfo(
    val name: String,
    val model: String? = null,
    val size: Long? = null
)
