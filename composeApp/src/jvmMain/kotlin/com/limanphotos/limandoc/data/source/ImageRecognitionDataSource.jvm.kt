package com.limanphotos.limandoc.data.source

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.limanphotos.limandoc.data.repository.AnalysisCacheRepository
import com.limanphotos.limandoc.domain.model.ImageAnalysis
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.prefs.Preferences
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
actual class ImageRecognitionDataSource {

    private val modelName = "llava" // Default vision model, user can change this
    private val ollamaBaseUrl = "http://localhost:11434"
    private val settings = PreferencesSettings(Preferences.userNodeForPackage(this::class.java))
    private val cacheRepository = AnalysisCacheRepository(settings)

    actual suspend fun analyzeImage(imagePath: String): ImageAnalysis =
        withContext(Dispatchers.IO) {
            try {
                val imageFile = File(imagePath)
                if (!imageFile.exists()) {
                    return@withContext ImageAnalysis(
                        description = "",
                        error = "Image file not found: $imagePath"
                    )
                }

                // Validate image format
                val validExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff")
                val fileExtension = imageFile.extension.lowercase()
                if (fileExtension !in validExtensions) {
                    return@withContext ImageAnalysis(
                        description = "",
                        error = "Unsupported image format: $fileExtension"
                    )
                }

                // Check if we have a valid cached analysis first
                val cachedAnalysis = cacheRepository.getCachedAnalysis(imagePath)
                if (cachedAnalysis != null) {
                    println("✅ Using cached analysis for $imagePath")
                    return@withContext cachedAnalysis.toImageAnalysis()
                }

                println("🔍 No valid cache found, analyzing image with LLaVA: $imagePath")

                // Try to use the REST API directly since ollama4j vision API has issues
                val description = try {
                    analyzeImageWithRestAPI(imageFile)
                } catch (e: Exception) {
                    // Fallback error handling
                    when {
                        e.message?.contains("Connection refused") == true -> {
                            return@withContext ImageAnalysis(
                                description = "",
                                error = "Ollama service not running. Start with: 'ollama serve'"
                            )
                        }

                        e.message?.contains("404") == true -> {
                            return@withContext ImageAnalysis(
                                description = "",
                                error = "Vision model not found. Install with: 'ollama pull llava'"
                            )
                        }

                        else -> {
                            return@withContext ImageAnalysis(
                                description = "",
                                error = "Ollama connection failed: ${e.message}"
                            )
                        }
                    }
                }

                if (description.isNotBlank()) {
                    // Cache the successful analysis
                    cacheRepository.cacheAnalysis(imagePath, description)
                    println("💾 Cached analysis for $imagePath")

                    ImageAnalysis(
                        description = description,
                        tags = emptyList() // Tags will be extracted at collection level from actual keywords
                    )
                } else {
                    ImageAnalysis(
                        description = "",
                        error = "No response from Ollama service"
                    )
                }

            } catch (e: Exception) {
                ImageAnalysis(
                    description = "",
                    error = "Failed to analyze image: ${e.message}"
                )
            }
        }

    private fun analyzeImageWithRestAPI(imageFile: File): String {
        // Encode image to base64
        val imageBytes = imageFile.readBytes()
        val base64Image = Base64.getEncoder().encodeToString(imageBytes)

        // TODO use this for normal searching through keywords
//        val prompt = """
//            Describe this image in keywords. What do you see?
//            Include objects, people, settings, colors, actions, and any other notable features only if they are present. Keywords only, no full sentences.
//        """.trimIndent()

        // TODO use this for collections screen,
        val prompt = """
            "Describe this image in keywords only.
            Output format (strict):
            objects: [comma-separated list]
            people: [comma-separated list]
            actions: [comma-separated list]
            emotions: [comma-separated list]
            settings: [comma-separated list]
            
            Rules:
            Use singular nouns.
            Prefer common words (car not automobile, kid not child).
            List at most 25 keywords total.
            Do not use full sentences. Do not invent details."
        """.trimIndent()

        // Create JSON payload
        val mapper = ObjectMapper()
        val payload = mapper.createObjectNode().apply {
            put("model", modelName)
            put("prompt", prompt)
            putArray("images").add(base64Image)
            put("stream", false)
        }

        // Make HTTP request
        val url = URL("$ollamaBaseUrl/api/generate")
        val connection = url.openConnection() as HttpURLConnection

        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }

        // Send request
        connection.outputStream.use { os ->
            os.write(payload.toString().toByteArray())
        }

        // Read response
        val responseCode = connection.responseCode
        if (responseCode != 200) {
            throw RuntimeException("HTTP $responseCode: ${connection.responseMessage}")
        }

        val responseText = connection.inputStream.bufferedReader().use { it.readText() }

        // Parse JSON response
        val responseJson = mapper.readTree(responseText) as ObjectNode
        return responseJson.get("response")?.asText() ?: ""
    }


}