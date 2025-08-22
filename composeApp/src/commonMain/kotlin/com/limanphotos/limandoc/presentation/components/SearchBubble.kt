package com.limanphotos.limandoc.presentation.components

import androidx.compose.runtime.Immutable

/**
 * Represents a search term bubble that can be a single word or a quoted phrase
 */
@Immutable
data class SearchBubble(
    val id: String,
    val content: String,
    val isPhrase: Boolean = false, // true if it's a quoted phrase like "two women"
    val isEditing: Boolean = false
) {
    /**
     * Get the search query representation of this bubble
     */
    fun toSearchQuery(): String {
        return if (isPhrase) {
            "\"$content\""
        } else {
            content
        }
    }

    /**
     * Get the display text for the bubble
     */
    fun getDisplayText(): String {
        return if (isPhrase) {
            "\"$content\""
        } else {
            content
        }
    }

    companion object {
        /**
         * Create a word bubble
         */
        fun word(content: String): SearchBubble {
            return SearchBubble(
                id = generateId(),
                content = content,
                isPhrase = false
            )
        }

        /**
         * Create a phrase bubble
         */
        fun phrase(content: String): SearchBubble {
            return SearchBubble(
                id = generateId(),
                content = content,
                isPhrase = true
            )
        }

        private fun generateId(): String {
            return "bubble_${kotlin.random.Random.nextLong()}_${kotlin.random.Random.nextInt(1000)}"
        }
    }
}

/**
 * State holder for search bubbles and current input
 */
@Immutable
data class SearchBubbleState(
    val bubbles: List<SearchBubble> = emptyList(),
    val currentInput: String = "",
    val editingBubbleId: String? = null,
    val filters: PhotoFilters = PhotoFilters(
        fileTypeFilter = FileTypeFilter(setOf("jpg", "jpeg", "png"))
    ),
    val filterUIState: FilterUIState = FilterUIState()
) {
    /**
     * Get the complete search query from all bubbles and current input
     */
    fun getCompleteQuery(): String {
        val bubbleQueries = bubbles.map { it.toSearchQuery() }
        val allParts = if (currentInput.isNotBlank()) {
            bubbleQueries + currentInput.trim()
        } else {
            bubbleQueries
        }
        return allParts.joinToString(" ")
    }

    /**
     * Check if we're currently editing any bubble
     */
    fun isEditingAnyBubble(): Boolean {
        return editingBubbleId != null
    }

    /**
     * Check if any filters are active
     */
    fun hasActiveFilters(): Boolean {
        return !filters.isEmpty()
    }

    /**
     * Get the bubble being edited, if any
     */
    fun getEditingBubble(): SearchBubble? {
        return editingBubbleId?.let { id ->
            bubbles.find { it.id == id }
        }
    }
}