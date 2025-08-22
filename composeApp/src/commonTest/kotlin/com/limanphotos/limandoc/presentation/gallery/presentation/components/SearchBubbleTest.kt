package com.limanphotos.limandoc.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchBubbleTest {

    @Test
    fun `SearchBubble word creates correct bubble`() {
        val bubble = SearchBubble.word("golden")

        assertEquals("golden", bubble.content)
        assertFalse(bubble.isPhrase)
        assertFalse(bubble.isEditing)
        assertEquals("golden", bubble.toSearchQuery())
        assertEquals("golden", bubble.getDisplayText())
    }

    @Test
    fun `SearchBubble phrase creates correct bubble`() {
        val bubble = SearchBubble.phrase("two women")

        assertEquals("two women", bubble.content)
        assertTrue(bubble.isPhrase)
        assertFalse(bubble.isEditing)
        assertEquals("\"two women\"", bubble.toSearchQuery())
        assertEquals("\"two women\"", bubble.getDisplayText())
    }

    @Test
    fun `SearchBubble generates unique IDs`() {
        val bubble1 = SearchBubble.word("test")
        val bubble2 = SearchBubble.word("test")

        assertTrue(bubble1.id != bubble2.id)
    }
}

class SearchBubbleStateTest {

    @Test
    fun `getCompleteQuery combines bubbles and current input`() {
        val bubbles = listOf(
            SearchBubble.word("golden"),
            SearchBubble.phrase("two women")
        )
        val state = SearchBubbleState(
            bubbles = bubbles,
            currentInput = "beautiful"
        )

        val query = state.getCompleteQuery()
        assertEquals("golden \"two women\" beautiful", query)
    }

    @Test
    fun `getCompleteQuery handles empty current input`() {
        val bubbles = listOf(
            SearchBubble.word("golden"),
            SearchBubble.phrase("sunset")
        )
        val state = SearchBubbleState(bubbles = bubbles)

        val query = state.getCompleteQuery()
        assertEquals("golden \"sunset\"", query)
    }

    @Test
    fun `getCompleteQuery handles empty bubbles`() {
        val state = SearchBubbleState(currentInput = "golden sunset")

        val query = state.getCompleteQuery()
        assertEquals("golden sunset", query)
    }

    @Test
    fun `getCompleteQuery handles completely empty state`() {
        val state = SearchBubbleState()

        val query = state.getCompleteQuery()
        assertEquals("", query)
    }

    @Test
    fun `isEditingAnyBubble returns correct status`() {
        val state1 = SearchBubbleState()
        assertFalse(state1.isEditingAnyBubble())

        val state2 = SearchBubbleState(editingBubbleId = "bubble_123")
        assertTrue(state2.isEditingAnyBubble())
    }

    @Test
    fun `getEditingBubble returns correct bubble`() {
        val bubble1 = SearchBubble.word("golden")
        val bubble2 = SearchBubble.word("sunset")
        val bubbles = listOf(bubble1, bubble2)

        val state = SearchBubbleState(
            bubbles = bubbles,
            editingBubbleId = bubble2.id
        )

        val editingBubble = state.getEditingBubble()
        assertEquals(bubble2, editingBubble)
    }

    @Test
    fun `getEditingBubble returns null when not editing`() {
        val bubble = SearchBubble.word("golden")
        val state = SearchBubbleState(bubbles = listOf(bubble))

        val editingBubble = state.getEditingBubble()
        assertEquals(null, editingBubble)
    }
}