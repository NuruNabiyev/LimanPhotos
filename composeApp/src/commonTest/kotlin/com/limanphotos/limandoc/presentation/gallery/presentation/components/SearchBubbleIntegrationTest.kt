package com.limanphotos.limandoc.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests that simulate real user interactions with the bubble search
 */
class SearchBubbleIntegrationTest {

    @Test
    fun `user types single word and presses space`() {
        // Simulate: User types "golden "
        val input = "golden "
        val (bubbles, remaining) = SearchBubbleParser.parseInput(input)

        // Should create one word bubble
        assertEquals(1, bubbles.size)
        assertEquals("golden", bubbles[0].content)
        assertFalse(bubbles[0].isPhrase)
        assertEquals("", remaining)
    }

    @Test
    fun `user types phrase with quotes`() {
        // Simulate: User types "two women" (with quotes and space)
        val input = "\"two women\" "
        val (bubbles, remaining) = SearchBubbleParser.parseInput(input)

        // Should create one phrase bubble
        assertEquals(1, bubbles.size)
        assertEquals("two women", bubbles[0].content)
        assertTrue(bubbles[0].isPhrase)
        assertEquals("", remaining)
    }

    @Test
    fun `user builds complex query step by step`() {
        var state = SearchBubbleState()

        // Step 1: User types "golden "
        val (bubbles1, remaining1) = SearchBubbleParser.parseInput("golden ")
        state = state.copy(bubbles = state.bubbles + bubbles1, currentInput = remaining1)
        assertEquals("golden", state.getCompleteQuery())

        // Step 2: User types "\"two women\" "
        val (bubbles2, remaining2) = SearchBubbleParser.parseInput("\"two women\" ")
        state = state.copy(bubbles = state.bubbles + bubbles2, currentInput = remaining2)
        assertEquals("golden \"two women\"", state.getCompleteQuery())

        // Step 3: User types "beautiful" (no space yet)
        state = state.copy(currentInput = "beautiful")
        assertEquals("golden \"two women\" beautiful", state.getCompleteQuery())

        // Step 4: User adds space to complete the word
        val (bubbles3, remaining3) = SearchBubbleParser.parseInput("beautiful ")
        state = state.copy(bubbles = state.bubbles + bubbles3, currentInput = remaining3)
        assertEquals("golden \"two women\" beautiful", state.getCompleteQuery())
    }

    @Test
    fun `user removes bubbles with backspace simulation`() {
        // Start with some bubbles
        val initialBubbles = listOf(
            SearchBubble.word("golden"),
            SearchBubble.phrase("two women"),
            SearchBubble.word("beautiful")
        )
        var state = SearchBubbleState(bubbles = initialBubbles)

        // Simulate backspace when no current input (should edit last bubble)
        assertTrue(state.currentInput.isEmpty())
        assertTrue(state.bubbles.isNotEmpty())

        // Remove last bubble and put its content in current input
        val lastBubble = state.bubbles.last()
        state = state.copy(
            bubbles = state.bubbles.dropLast(1),
            currentInput = lastBubble.content
        )

        assertEquals(2, state.bubbles.size)
        assertEquals("beautiful", state.currentInput)
        assertEquals("golden \"two women\" beautiful", state.getCompleteQuery())
    }

    @Test
    fun `user edits existing bubble`() {
        val bubble = SearchBubble.word("golden")
        var state = SearchBubbleState(bubbles = listOf(bubble))

        // User clicks on bubble to edit
        state = state.copy(editingBubbleId = bubble.id)
        assertTrue(state.isEditingAnyBubble())
        assertEquals(bubble, state.getEditingBubble())

        // User modifies the bubble content
        val editedBubble = bubble.copy(content = "golden sunset", isEditing = true)
        state = state.copy(
            bubbles = listOf(editedBubble),
            editingBubbleId = null
        )

        assertEquals("golden sunset", state.bubbles[0].content)
        assertFalse(state.isEditingAnyBubble())
    }

    @Test
    fun `user types mixed content in one go`() {
        // Simulate: User pastes or types complex query all at once
        val input = "\"two women\" golden 'formal dress' beautiful "
        val (bubbles, remaining) = SearchBubbleParser.parseInput(input)

        val state = SearchBubbleState(bubbles = bubbles, currentInput = remaining)

        assertEquals(4, bubbles.size)
        assertEquals("", remaining)

        // Verify the complete query is reconstructed correctly
        val expectedQuery = "\"two women\" golden \"formal dress\" beautiful"
        assertEquals(expectedQuery, state.getCompleteQuery())
    }

    @Test
    fun `user removes bubble by clicking X`() {
        val bubble1 = SearchBubble.word("golden")
        val bubble2 = SearchBubble.phrase("two women")
        val bubble3 = SearchBubble.word("beautiful")

        var state = SearchBubbleState(bubbles = listOf(bubble1, bubble2, bubble3))

        // User clicks X on the middle bubble (two women)
        state = state.copy(bubbles = state.bubbles.filter { it.id != bubble2.id })

        assertEquals(2, state.bubbles.size)
        assertEquals("golden beautiful", state.getCompleteQuery())
        assertFalse(state.bubbles.any { it.content == "two women" })
    }

    @Test
    fun `user starts typing phrase but doesn't complete it`() {
        // User types opening quote but hasn't finished the phrase
        val input = "\"two wo"
        val (bubbles, remaining) = SearchBubbleParser.parseInput(input)

        // Should not create any bubbles yet
        assertEquals(0, bubbles.size)
        assertEquals("\"two wo", remaining)

        // Parser should detect incomplete phrase
        assertTrue(SearchBubbleParser.hasIncompletePhrase(input))
    }

    @Test
    fun `search query generation with various bubble types`() {
        val bubbles = listOf(
            SearchBubble.word("golden"),           // -> "golden"
            SearchBubble.phrase("two women"),      // -> "\"two women\""
            SearchBubble.word("sunset"),           // -> "sunset"
            SearchBubble.phrase("formal dress")    // -> "\"formal dress\""
        )

        val state = SearchBubbleState(
            bubbles = bubbles,
            currentInput = "beautiful"
        )

        val query = state.getCompleteQuery()
        assertEquals("golden \"two women\" sunset \"formal dress\" beautiful", query)
    }
}