package com.example.core.prompt

import com.example.core.model.*

sealed class PromptValidationResult {
    object Valid : PromptValidationResult()
    data class Invalid(val reason: String) : PromptValidationResult()
}

data class ProcessedPrompt(
    val originalPrompt: String,
    val enhancedPrompt: String,
    val effectiveNegativePrompt: String,
    val style: ImageStyle,
    val aspectRatio: CreationAspectRatio,
    val quality: GenerationQuality,
    val seed: Long,
    val guidanceScale: Float
)

class PromptProcessor {

    fun validatePrompt(prompt: String): PromptValidationResult {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            return PromptValidationResult.Invalid("Prompt cannot be empty. Describe what you'd like to create.")
        }
        if (trimmed.length < 3) {
            return PromptValidationResult.Invalid("Prompt is too brief. Please enter at least 3 characters.")
        }
        if (trimmed.length > 1000) {
            return PromptValidationResult.Invalid("Prompt exceeds the 1,000 character maximum limit.")
        }
        return PromptValidationResult.Valid
    }

    fun process(
        request: CreationRequest,
        enableEnhancement: Boolean = true
    ): ProcessedPrompt {
        val cleanPrompt = request.prompt.trim()

        val enhanced = if (enableEnhancement) {
            enhancePrompt(cleanPrompt, request.style, request.animePreset, request.mode)
        } else {
            cleanPrompt
        }

        val effectiveNegative = if (request.negativePrompt.isNotBlank()) {
            request.negativePrompt.trim()
        } else {
            buildDefaultNegativePrompt(request.style, request.mode)
        }

        val finalSeed = request.seed ?: kotlin.random.Random.nextLong(1, 999999999)
        val guidance = when (request.quality) {
            GenerationQuality.DRAFT -> 6.5f
            GenerationQuality.STANDARD -> 7.5f
            GenerationQuality.HIGH -> 8.5f
        }

        return ProcessedPrompt(
            originalPrompt = cleanPrompt,
            enhancedPrompt = enhanced,
            effectiveNegativePrompt = effectiveNegative,
            style = request.style,
            aspectRatio = request.aspectRatio,
            quality = request.quality,
            seed = finalSeed,
            guidanceScale = guidance
        )
    }

    fun enhancePrompt(
        basePrompt: String,
        style: ImageStyle,
        animePreset: AnimePreset,
        mode: CreationMode
    ): String {
        val parts = mutableListOf<String>()
        parts.add(basePrompt)

        when (mode) {
            CreationMode.ANIME -> {
                if (animePreset != AnimePreset.CUSTOM && animePreset.basePrompt.isNotBlank()) {
                    parts.add(animePreset.basePrompt)
                }
                parts.add(style.promptModifier)
                parts.add("expressive character design, sharp focus, vibrant palette, cinematic anime lighting")
            }
            CreationMode.THREE_D -> {
                parts.add("3D volumetric model, clean watertight mesh topology, 360 degree turnaround view, ambient occlusion, centered isometric angle")
            }
            CreationMode.IMAGE, CreationMode.EDIT -> {
                parts.add(style.promptModifier)
            }
        }

        return parts.joinToString(", ")
    }

    private fun buildDefaultNegativePrompt(style: ImageStyle, mode: CreationMode): String {
        return when (mode) {
            CreationMode.THREE_D -> "2d flat image, background clutter, severed geometry, low polygon count, non-manifold edges, blurry"
            CreationMode.ANIME -> "photorealistic real human, 3d render, western comic style, low quality, oversaturated, deformed anatomy, bad hands"
            else -> "blurry, low quality, distorted, extra limbs, bad proportions, watermark, artifacts, pixelated"
        }
    }
}
