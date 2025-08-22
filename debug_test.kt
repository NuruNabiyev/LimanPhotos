// Quick debug test for bubble parser
import com.limanphotos.limandoc.presentation.components.SearchBubbleParser

fun main() {
    println("=== Testing Bubble Parser ===")
    
    // Test 1: "gold " should create bubble
    println("\nTest 1: 'gold ' (with space)")
    val (bubbles1, remaining1) = SearchBubbleParser.parseInput("gold ")
    println("Bubbles: ${bubbles1.size}, Remaining: '$remaining1'")
    if (bubbles1.isNotEmpty()) {
        println("First bubble: '${bubbles1[0].content}', isPhrase: ${bubbles1[0].isPhrase}")
    }
    
    // Test 2: "\"two " should NOT create bubble
    println("\nTest 2: '\"two ' (incomplete quote)")
    val (bubbles2, remaining2) = SearchBubbleParser.parseInput("\"two ")
    println("Bubbles: ${bubbles2.size}, Remaining: '$remaining2'")
    
    // Test 3: Check hasIncompletePhrase
    println("\nTest 3: hasIncompletePhrase checks")
    println("'\"two ': ${SearchBubbleParser.hasIncompletePhrase("\"two ")}")
    println("'gold ': ${SearchBubbleParser.hasIncompletePhrase("gold ")}")
    
    // Test 4: "\"two women\" " should create phrase bubble
    println("\nTest 4: '\"two women\" ' (complete phrase)")
    val (bubbles4, remaining4) = SearchBubbleParser.parseInput("\"two women\" ")
    println("Bubbles: ${bubbles4.size}, Remaining: '$remaining4'")
    if (bubbles4.isNotEmpty()) {
        println("First bubble: '${bubbles4[0].content}', isPhrase: ${bubbles4[0].isPhrase}")
    }
}