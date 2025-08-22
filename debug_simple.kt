import kotlin.test.assertEquals

fun main() {
    // Manually test the exact case that's failing
    val input = "gold "
    
    println("=== Testing: '$input' ===")
    
    // Step 1: Test split logic
    val parts = input.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    println("Split parts: $parts")
    println("Parts size: ${parts.size}")
    
    // Step 2: Test endsWith
    val endsWithSpace = input.endsWith(" ")
    println("Ends with space: $endsWithSpace")
    
    // Step 3: Simulate bubble creation
    val bubbles = mutableListOf<String>()
    if (endsWithSpace) {
        parts.forEach { word ->
            if (word.isNotEmpty()) {
                bubbles.add(word)
            }
        }
    }
    
    println("Would create bubbles: $bubbles")
    println("Expected 1, got ${bubbles.size}")
}