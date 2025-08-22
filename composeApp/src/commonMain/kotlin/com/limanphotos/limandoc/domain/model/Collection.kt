package com.limanphotos.limandoc.domain.model

/**
 * Represents a collection of photos grouped by a specific tag
 */
data class Collection(
    val tag: String,
    val category: TagCategory,
    val photos: List<Photo>,
    val photoCount: Int = photos.size
) {
    /**
     * Get preview photos (max 6) for display
     */
    fun getPreviewPhotos(): List<Photo> {
        return photos.take(6)
    }
}

/**
 * Categories for organizing tags from AI analysis
 */
enum class TagCategory() {
    OBJECTS(),
    PEOPLE(),
    ACTIONS(),
    EMOTIONS(),
    SETTINGS()
}

