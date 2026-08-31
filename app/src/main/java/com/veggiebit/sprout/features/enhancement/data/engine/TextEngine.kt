package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult

/**
 * Contract for text transformation engines.
 */
interface TextEngine {
    suspend fun transform(payload: TextPayload, preset: TransformPreset): TransformResult
}
