@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.data.repository

import java.io.File
import kotlin.time.Instant

/**
 * JVM implementation for getting file modification time
 */
actual fun getFileModificationTimeImpl(imagePath: String): Instant? {
    return try {
        val file = File(imagePath)
        if (file.exists()) {
            Instant.fromEpochMilliseconds(file.lastModified())
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}