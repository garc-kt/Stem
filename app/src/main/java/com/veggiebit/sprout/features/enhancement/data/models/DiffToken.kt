package com.veggiebit.sprout.features.enhancement.data.models

enum class DiffType {
    ADDED,
    DELETED,
    UNMODIFIED
}

/**
 * Represents a token or substring segment within a visual diff comparison.
 */
data class DiffToken(
    val text: String,
    val type: DiffType
)
