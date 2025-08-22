// Simple debug for bubble parser
fun main() {
    println("=== Debug Bubble Parser ===")
    
    val input = "gold "
    println("Testing input: '$input'")
    println("Input ends with space: ${input.endsWith(" ")}")
    
    val parts = input.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    println("Split parts: $parts")
    println("Parts size: ${parts.size}")
    
    if (parts.isNotEmpty()) {
        println("First part: '${parts[0]}'")
    }
}