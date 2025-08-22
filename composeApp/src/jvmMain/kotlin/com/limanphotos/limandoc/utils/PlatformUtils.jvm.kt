package com.limanphotos.limandoc.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.imageio.ImageIO

actual object PlatformUtils {

    actual suspend fun executeCommand(command: String): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                val parts = command.split("\\s+".toRegex())
                val process = ProcessBuilder(parts)
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                CommandResult(
                    isSuccess = exitCode == 0,
                    output = output.trim(),
                    error = if (exitCode != 0) "Process exited with code: $exitCode" else ""
                )
            } catch (e: Exception) {
                CommandResult(
                    isSuccess = false,
                    output = "",
                    error = e.message ?: "Unknown error executing command"
                )
            }
        }
    }

    actual fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                // Fallback for systems without Desktop support
                when (getOperatingSystem()) {
                    OperatingSystem.MACOS -> Runtime.getRuntime().exec("open $url")
                    OperatingSystem.WINDOWS -> Runtime.getRuntime()
                        .exec("rundll32 url.dll,FileProtocolHandler $url")

                    OperatingSystem.LINUX -> Runtime.getRuntime().exec("xdg-open $url")
                    else -> println("Cannot open URL: Desktop not supported")
                }
            }
        } catch (e: Exception) {
            println("Error opening URL: ${e.message}")
        }
    }

    private fun getOperatingSystem(): OperatingSystem {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("mac") || osName.contains("darwin") -> OperatingSystem.MACOS
            osName.contains("win") -> OperatingSystem.WINDOWS
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> OperatingSystem.LINUX
            else -> OperatingSystem.UNKNOWN
        }
    }

    /**
     * Get image dimensions without loading the full image into memory
     */
    fun getImageDimensions(file: File): Pair<Int, Int>? {
        try {
            ImageIO.createImageInputStream(file)?.use { input ->
                val readers = ImageIO.getImageReaders(input)
                if (readers.hasNext()) {
                    val reader = readers.next()
                    reader.input = input
                    val width = reader.getWidth(0)
                    val height = reader.getHeight(0)
                    reader.dispose()
                    return width to height
                }
            }
        } catch (e: Exception) {
            println("Error reading image dimensions for ${file.name}: ${e.message}")
        }
        return null
    }

    private enum class OperatingSystem {
        MACOS, WINDOWS, LINUX, UNKNOWN
    }
}