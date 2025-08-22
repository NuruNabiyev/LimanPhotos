package com.limanphotos.limandoc.presentation.components

/**
 * Utility class for parsing search input into bubbles
 */
object SearchBubbleParser {

    /**
     * Parse input text and extract bubbles when user presses space or completes phrases
     */
    fun parseInput(input: String): Pair<List<SearchBubble>, String> {
        if (input.isBlank()) {
            return Pair(emptyList(), "")
        }

        // Check if we're in the middle of typing a phrase (incomplete quotes)
        if (hasIncompletePhrase(input)) {
            // Don't extract anything, keep as remaining input
            return Pair(emptyList(), input)
        }

        val bubbles = mutableListOf<SearchBubble>()
        var remainingInput = input

        // Process input left-to-right to preserve order
        remainingInput = extractInOrder(remainingInput, bubbles)

        return Pair(bubbles, remainingInput)
    }

    /**
     * Extract bubbles in order, preserving the sequence of phrases and words
     */
    private fun extractInOrder(input: String, bubbles: MutableList<SearchBubble>): String {
        // Track all elements (phrases and words) with their positions
        val elements = mutableListOf<Pair<Int, SearchBubble>>() // position, bubble

        // Find all quoted phrases and their positions
        val quotedPatterns = listOf(
            "\"([^\"]+)\"\\s*".toRegex(),  // Double quotes
            "'([^']+)'\\s*".toRegex()      // Single quotes
        )

        quotedPatterns.forEach { pattern ->
            pattern.findAll(input).forEach { match ->
                val content = match.groupValues[1].trim()
                if (content.isNotEmpty()) {
                    elements.add(match.range.first to SearchBubble.phrase(content))
                }
            }
        }

        // Find completed words (those followed by space) in original input
        // but skip positions that are inside quoted phrases
        val quotedRanges = mutableListOf<IntRange>()
        quotedPatterns.forEach { pattern ->
            pattern.findAll(input).forEach { match ->
                quotedRanges.add(match.range)
            }
        }

        val wordPattern = "(\\S+)\\s+".toRegex() // Word followed by space
        wordPattern.findAll(input).forEach { match ->
            val word = match.groupValues[1].trim()
            val startPos = match.range.first

            // Only add if this word is not inside a quoted phrase
            val isInsideQuote = quotedRanges.any { range ->
                startPos >= range.first && startPos <= range.last
            }

            if (word.isNotEmpty() && !isInsideQuote) {
                elements.add(startPos to SearchBubble.word(word))
            }
        }

        // Sort by position and add to bubbles in order
        elements.sortBy { it.first }
        elements.forEach { bubbles.add(it.second) }

        // Handle any incomplete word at the end
        // Remove all processed elements from input to find remaining
        var remainingInput = input
        quotedPatterns.forEach { pattern ->
            remainingInput = pattern.replace(remainingInput, "")
        }

        // Remove completed words
        remainingInput = wordPattern.replace(remainingInput, "")

        return remainingInput.trim()
    }

    /**
     * Check if input contains a potential phrase being typed
     */
    fun hasIncompletePhrase(input: String): Boolean {
        val doubleQuoteCount = input.count { it == '"' }
        val singleQuoteCount = input.count { it == '\'' }

        // Odd number of quotes means we're in the middle of typing a phrase
        return (doubleQuoteCount % 2 == 1) || (singleQuoteCount % 2 == 1)
    }

    /**
     * Detect if user is trying to type a phrase with quotes
     */
    fun detectPhraseIntent(input: String): Boolean {
        return input.contains('"') || input.contains('\'')
    }

}