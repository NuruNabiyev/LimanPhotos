package com.limanphotos.limandoc.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.limanphotos.limandoc.utils.DesktopApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

actual suspend fun loadImageBitmap(imagePath: String): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        val file = File(imagePath)
        if (!file.exists()) return@withContext null

        val originalImage = ImageIO.read(file) ?: return@withContext null

        // TODO 2 scales, also in PhotoItem.kt
        // Resize image to maximum 300x300 for better performance
        //val scaledImage = scaleImage(originalImage, 300, 300)

        originalImage.toComposeImageBitmap()
    } catch (e: Exception) {
        println("Error loading image $imagePath: ${e.message}")
        null
    }
}

actual fun ImageBitmap.scale(scaleFactor: Float): ImageBitmap {
    if (scaleFactor <= 0f || scaleFactor > 1f) {
        throw IllegalArgumentException("Scale factor must be between 0 and 1")
    }

    if (scaleFactor == 1f) return this

    // Convert ImageBitmap to BufferedImage
    val awtImage = this.toAwtImage()
    val bufferedImage = if (awtImage is BufferedImage) {
        awtImage
    } else {
        val newBufferedImage = BufferedImage(
            awtImage.getWidth(null),
            awtImage.getHeight(null),
            BufferedImage.TYPE_INT_ARGB
        )
        val graphics = newBufferedImage.createGraphics()
        graphics.drawImage(awtImage, 0, 0, null)
        graphics.dispose()
        newBufferedImage
    }

    val originalWidth = bufferedImage.width
    val originalHeight = bufferedImage.height

    val newWidth = (originalWidth * scaleFactor).toInt()
    val newHeight = (originalHeight * scaleFactor).toInt()

    if (newWidth <= 0 || newHeight <= 0) {
        throw IllegalArgumentException("Scaled dimensions must be positive")
    }

    val scaledImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
    val graphics = scaledImage.createGraphics()

    // Use fast scaling for better performance
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR
    )
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
    graphics.setRenderingHint(
        RenderingHints.KEY_COLOR_RENDERING,
        RenderingHints.VALUE_COLOR_RENDER_SPEED
    )

    graphics.drawImage(bufferedImage, 0, 0, newWidth, newHeight, null)
    graphics.dispose()

    return scaledImage.toComposeImageBitmap()
}

@Composable
actual fun RightClickMenu(filepath: String, content: @Composable (() -> Unit)) {
    ContextMenuArea(items = {
        listOf(
            ContextMenuItem("Open externally") {
                DesktopApi.open(File(filepath))
            },
            ContextMenuItem("Find in File Explorer") {
                DesktopApi.browse(File(filepath).toURI())
            }
        )
    }) {
        content()
    }
}