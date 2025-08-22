package com.limanphotos.limandoc.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchBubbleParserTest {

    @Test
    fun `parseInput creates word bubble when input ends with space`() {
        val (bubbles, remaining) = SearchBubbleParser.parseInput("hello ")

        assertEquals(1, bubbles.size)
        assertEquals("hello", bubbles[0].content)
        assertFalse(bubbles[0].isPhrase)
        assertEquals("", remaining)
    }

    @Test
    fun `parseInput creates word bubble for gold with space - user reported bug`() {
        // This is the exact case the user reported that wasn't working
        val (bubbles, remaining) = SearchBubbleParser.parseInput("gold ")

        assertEquals(1, bubbles.size, "Should create one bubble for 'gold '")
        assertEquals("gold", bubbles[0].content)
        assertFalse(bubbles[0].isPhrase)
        assertEquals("", remaining)
    }

    @Test
    fun `parseInput does NOT create bubble for incomplete quote - user reported bug`() {
        // This is the exact case the user reported - "two should not create a bubble
        val (bubbles, remaining) = SearchBubbleParser.parseInput("\"two ")

        assertEquals(0, bubbles.size, "Should NOT create bubble for incomplete quote")
        assertEquals("\"two ", remaining, "Should keep incomplete quote as remaining input")
    }

    @Test
    fun `parseInput creates multiple word bubbles`() {
        val (bubbles, remaining) = SearchBubbleParser.parseInput("hello world ")

        assertEquals(2, bubbles.size)
        assertEquals("hello", bubbles[0].content)
        assertEquals("world", bubbles[1].content)
        assertFalse(bubbles[0].isPhrase)
        assertFalse(bubbles[1].isPhrase)
        assertEquals("", remaining)
    }

    @Test
    fun `parseInput keeps incomplete word as remaining input`() {
        val (bubbles, remaining) = SearchBubbleParser.parseInput("hello world")

        assertEquals(1, bubbles.size)
        assertEquals("hello", bubbles[0].content)
        assertEquals("world", remaining)
    }

    @Test
    fun `parseInput creates phrase bubble from double quotes`() {
        val (bubbles, remaining) = SearchBubbleParser.parseInput("\"two women\" ")

        assertEquals(1, bubbles.size)
        assertEquals("two women", bubbles[0].content)
        assertTrue(bubbles[0].isPhrase)
        assertEquals("", remaining)
    }

    @Test
    fun `parseInput creates phrase bubble from single quotes`() {
        val (bubbles, remaining) = SearchBubbleParser.parseInput("'golden sunset' ")

        assertEquals(1, bubbles.size)
        assertEquals("golden sunset", bubbles[0].content)
        assertTrue(bubbles[0].isPhrase)
        assertEquals("", remaining)
    }

    @Test
    fun `parseInput handles mixed phrases and words`() {
        val (bubbles, remaining) = SearchBubbleParser.parseInput("\"two women\" golden beautiful")

        assertEquals(2, bubbles.size)

        // First bubble should be the phrase
        assertEquals("two women", bubbles[0].content)
        assertTrue(bubbles[0].isPhrase)

        // Second bubble should be the completed word
        assertEquals("golden", bubbles[1].content)
        assertFalse(bubbles[1].isPhrase)

        // Last word should remain as input
        assertEquals("beautiful", remaining)
    }

    @Test
    fun `parseInput handles multiple phrases`() {
        val (bubbles, remaining) = SearchBubbleParser.parseInput("\"two women\" 'formal dress' ")

        assertEquals(2, bubbles.size)
        assertEquals("two women", bubbles[0].content)
        assertTrue(bubbles[0].isPhrase)
        assertEquals("formal dress", bubbles[1].content)
        assertTrue(bubbles[1].isPhrase)
        assertEquals("", remaining)
    }

    @Test
    fun `parseInput handles empty and whitespace input`() {
        val (bubbles1, remaining1) = SearchBubbleParser.parseInput("")
        assertEquals(0, bubbles1.size)
        assertEquals("", remaining1)

        val (bubbles2, remaining2) = SearchBubbleParser.parseInput("   ")
        assertEquals(0, bubbles2.size)
        assertEquals("", remaining2)
    }

    @Test
    fun `hasIncompletePhrase detects incomplete quotes`() {
        assertTrue(SearchBubbleParser.hasIncompletePhrase("\"hello"))
        assertTrue(SearchBubbleParser.hasIncompletePhrase("'hello"))
        assertTrue(SearchBubbleParser.hasIncompletePhrase("hello \"world"))

        assertFalse(SearchBubbleParser.hasIncompletePhrase("\"hello\""))
        assertFalse(SearchBubbleParser.hasIncompletePhrase("'hello'"))
        assertFalse(SearchBubbleParser.hasIncompletePhrase("hello world"))
    }

    @Test
    fun `detectPhraseIntent identifies quotes in input`() {
        assertTrue(SearchBubbleParser.detectPhraseIntent("\"hello"))
        assertTrue(SearchBubbleParser.detectPhraseIntent("'hello"))
        assertTrue(SearchBubbleParser.detectPhraseIntent("hello \"world\""))

        assertFalse(SearchBubbleParser.detectPhraseIntent("hello world"))
        assertFalse(SearchBubbleParser.detectPhraseIntent(""))
    }

    @Test
    fun `parseInput handles complex real-world scenarios`() {
        // Scenario: User types a complex query
        val input = "\"two women\" golden 'formal dress' beautiful "
        val (bubbles, remaining) = SearchBubbleParser.parseInput(input)

        assertEquals(4, bubbles.size)

        // Check each bubble
        assertEquals("two women", bubbles[0].content)
        assertTrue(bubbles[0].isPhrase)

        assertEquals("golden", bubbles[1].content)
        assertFalse(bubbles[1].isPhrase)

        assertEquals("formal dress", bubbles[2].content)
        assertTrue(bubbles[2].isPhrase)

        assertEquals("beautiful", bubbles[3].content)
        assertFalse(bubbles[3].isPhrase)

        assertEquals("", remaining)
    }

    @Test
    fun `parseInput handles incomplete phrases correctly`() {
        // User is typing a phrase but hasn't finished
        val (bubbles, remaining) = SearchBubbleParser.parseInput("\"two wo")

        assertEquals(0, bubbles.size)
        assertEquals("\"two wo", remaining)
    }

    @Test
    fun `parseInput handles extra spaces gracefully`() {
        val (bubbles, remaining) = SearchBubbleParser.parseInput("  hello   world  ")

        assertEquals(2, bubbles.size)
        assertEquals("hello", bubbles[0].content)
        assertEquals("world", bubbles[1].content)
        assertEquals("", remaining)
    }
}