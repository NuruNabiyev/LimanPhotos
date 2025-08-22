// Debug complex parsing
import com.limanphotos.limandoc.presentation.components.SearchBubbleParser

fun main() {
    val input = "\"two women\" golden 'formal dress' beautiful "
    println("Input: '$input'")
    
    val (bubbles, remaining) = SearchBubbleParser.parseInput(input)
    
    println("Bubbles found: ${bubbles.size}")
    bubbles.forEachIndexed { index, bubble ->
        println("  [$index]: '${bubble.content}' (isPhrase=${bubble.isPhrase})")
    }
    println("Remaining: '$remaining'")
    
    println("\nExpected order:")
    println("  [0]: 'two women' (isPhrase=true)")
    println("  [1]: 'golden' (isPhrase=false)")  
    println("  [2]: 'formal dress' (isPhrase=true)")
    println("  [3]: 'beautiful' (isPhrase=false)")
}