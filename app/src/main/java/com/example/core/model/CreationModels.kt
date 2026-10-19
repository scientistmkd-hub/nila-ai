package com.example.core.model

enum class CreationMode(val displayName: String) {
    IMAGE("Image"),
    ANIME("Anime"),
    THREE_D("3D Model"),
    EDIT("Edit")
}

enum class ImageStyle(val displayName: String, val promptModifier: String) {
    REALISTIC("Realistic", "photorealistic 8k uhd, high dynamic range, natural lighting, highly detailed"),
    CINEMATIC("Cinematic", "cinematic film still, dramatic volumetric lighting, 35mm photograph, masterwork"),
    THREE_D_STYLE("3D Render", "octane 3d render, ray tracing, subsurface scattering, stylized 3d asset"),
    ANIME("Anime", "masterpiece anime artwork, vibrant colors, expressive line art, Makoto Shinkai aesthetic"),
    CARTOON("Cartoon", "clean modern cartoon style, bold vector contours, playful expressive design"),
    FANTASY("Fantasy", "epic fantasy art, mythical ambiance, ethereal glowing elements, highly detailed digital painting"),
    SCI_FI("Sci-Fi", "futuristic science fiction concept art, cybernetic neon accents, sleek high-tech machinery"),
    MINIMAL("Minimal", "minimalist visual design, clean geometric forms, subtle palette, uncluttered negative space"),
    ILLUSTRATION("Illustration", "contemporary digital editorial illustration, stylized textures, artistic composition"),
    GAME_ASSET("Game Asset", "isometric video game prop, game-ready texture, clean silhouette, stylized asset"),
    PRODUCT("Product", "commercial studio product shot, soft diffused illumination, clean white backdrop, premium quality"),
    ARCHITECTURE("Architecture", "architectural visualization, modern structural design, ambient daylight, photorealistic render"),
    PORTRAIT("Portrait", "artistic character portrait, expressive focal depth, sharp focus on eyes, studio lighting")
}

enum class AnimePreset(val displayName: String, val basePrompt: String) {
    ANIME_CHARACTER("Anime Character", "Full body original anime character, dynamic pose, detailed clothing, futuristic aesthetic"),
    ANIME_PORTRAIT("Anime Portrait", "Close-up anime portrait, expressive eyes, intricate hair highlights, soft bokeh backdrop"),
    ANIME_BACKGROUND("Anime Background", "Scenery anime background, picturesque sky with clouds, vibrant lighting, Makoto Shinkai style"),
    ANIME_LANDSCAPE("Anime Landscape", "Vast anime fantasy landscape, rolling hills, ancient ruins, radiant sunbeams"),
    ANIME_ACTION("Anime Action", "Intense anime battle scene, dynamic motion blur, energy aura, dramatic camera angle"),
    ANIME_FANTASY("Anime Fantasy", "Magical anime setting, glowing runes, celestial spirits, mystical forest atmosphere"),
    ANIME_SCI_FI("Anime Sci-Fi", "Cyberpunk anime city, holographic signs, neon reflections on wet asphalt, high-tech vehicles"),
    ANIME_GAME_CONCEPT("Anime Game Concept", "Anime character concept sheet, multiple perspective turnarounds, accessory details"),
    CUSTOM("Custom", "")
}

enum class CreationAspectRatio(val label: String, val widthRatio: Float, val heightRatio: Float) {
    RATIO_1_1("1:1 Square", 1f, 1f),
    RATIO_4_5("4:5 Portrait", 4f, 5f),
    RATIO_3_4("3:4 Standard Portrait", 3f, 4f),
    RATIO_4_3("4:3 Standard Landscape", 4f, 3f),
    RATIO_16_9("16:9 Widescreen", 16f, 9f),
    RATIO_9_16("9:16 Vertical Story", 9f, 16f)
}

enum class GenerationQuality(val displayName: String, val stepCount: Int) {
    DRAFT("Draft (Fast)", 20),
    STANDARD("Standard (Balanced)", 30),
    HIGH("High (Max Detail)", 50)
}

enum class GenerationState {
    IDLE,
    PREPARING,
    QUEUED,
    GENERATING,
    FINALIZING,
    SUCCESS,
    FAILED,
    CANCELLED
}

enum class AssetType {
    IMAGE_2D,
    REAL_3D_MODEL,
    THREE_D_PREVIEW,
    EDITED_IMAGE
}

enum class ThreeDFormat(val extension: String) {
    GLB("glb"),
    GLTF("gltf"),
    OBJ("obj"),
    NONE("")
}

enum class CreationCollection(val displayName: String) {
    ALL("All Creations"),
    FAVORITES("Favorites"),
    CHARACTERS("Characters"),
    PROJECTS("Projects"),
    STUDY("Study"),
    GAME_ASSETS("Game Assets"),
    PERSONAL("Personal")
}

data class CreationRequest(
    val prompt: String,
    val negativePrompt: String = "",
    val style: ImageStyle = ImageStyle.REALISTIC,
    val animePreset: AnimePreset = AnimePreset.CUSTOM,
    val aspectRatio: CreationAspectRatio = CreationAspectRatio.RATIO_1_1,
    val quality: GenerationQuality = GenerationQuality.STANDARD,
    val seed: Long? = null,
    val mode: CreationMode = CreationMode.IMAGE,
    val sourceImageUri: String? = null,
    val editInstruction: String? = null,
    val gesturePose: HandGesture? = null
)

data class CreationResponse(
    val assetId: String,
    val assetUri: String,
    val thumbnailUri: String? = null,
    val meshUri: String? = null,
    val width: Int = 1024,
    val height: Int = 1024,
    val assetType: AssetType = AssetType.IMAGE_2D,
    val threeDFormat: ThreeDFormat = ThreeDFormat.NONE,
    val provider: String,
    val model: String,
    val prompt: String,
    val negativePrompt: String = "",
    val generationTimeMs: Long = 0L,
    val isReal3D: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
    val progress: Float? = null
)
