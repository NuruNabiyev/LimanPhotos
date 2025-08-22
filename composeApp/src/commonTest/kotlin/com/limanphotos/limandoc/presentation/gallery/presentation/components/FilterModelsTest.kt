package com.limanphotos.limandoc.presentation.components

import com.limanphotos.limandoc.domain.model.Photo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class FilterModelsTest {

    // Test photos with different sizes and formats
    private val smallJpeg = Photo(
        id = "1", path = "/test/small.jpg", name = "small.jpg",
        creationTime = Instant.fromEpochMilliseconds(0), size = 1024 * 1024, // 1MB
        extension = "jpg"
    )

    private val largeJpeg = Photo(
        id = "2", path = "/test/large.jpg", name = "large.jpg",
        creationTime = Instant.fromEpochMilliseconds(0), size = 10 * 1024 * 1024, // 10MB
        extension = "jpg"
    )

    private val smallPng = Photo(
        id = "3", path = "/test/small.png", name = "small.png",
        creationTime = Instant.fromEpochMilliseconds(0), size = 2 * 1024 * 1024, // 2MB
        extension = "png"
    )

    private val hugeRaw = Photo(
        id = "4", path = "/test/huge.raw", name = "huge.raw",
        creationTime = Instant.fromEpochMilliseconds(0), size = 100 * 1024 * 1024, // 100MB
        extension = "raw"
    )

    private val tinyBmp = Photo(
        id = "5", path = "/test/tiny.bmp", name = "tiny.bmp",
        creationTime = Instant.fromEpochMilliseconds(0), size = 500 * 1024, // 500KB
        extension = "bmp"
    )

    @Test
    fun `FileSizeFilter EMPTY matches all files`() {
        val filter = FileSizeFilter.EMPTY

        assertTrue(filter.isEmpty())
        assertTrue(filter.matches(smallJpeg.size))
        assertTrue(filter.matches(largeJpeg.size))
        assertTrue(filter.matches(hugeRaw.size))
        assertTrue(filter.matches(tinyBmp.size))
    }

    @Test
    fun `FileSizeFilter with minimum only`() {
        val filter = FileSizeFilter(minSizeBytes = 2 * 1024 * 1024) // 2MB min

        assertFalse(filter.isEmpty())
        assertFalse(filter.matches(smallJpeg.size)) // 1MB < 2MB
        assertTrue(filter.matches(largeJpeg.size)) // 10MB >= 2MB
        assertTrue(filter.matches(smallPng.size)) // 2MB >= 2MB
        assertTrue(filter.matches(hugeRaw.size)) // 100MB >= 2MB
        assertFalse(filter.matches(tinyBmp.size)) // 500KB < 2MB
    }

    @Test
    fun `FileSizeFilter with maximum only`() {
        val filter = FileSizeFilter(maxSizeBytes = 5 * 1024 * 1024) // 5MB max

        assertFalse(filter.isEmpty())
        assertTrue(filter.matches(smallJpeg.size)) // 1MB <= 5MB
        assertFalse(filter.matches(largeJpeg.size)) // 10MB > 5MB
        assertTrue(filter.matches(smallPng.size)) // 2MB <= 5MB
        assertFalse(filter.matches(hugeRaw.size)) // 100MB > 5MB
        assertTrue(filter.matches(tinyBmp.size)) // 500KB <= 5MB
    }

    @Test
    fun `FileSizeFilter with both min and max`() {
        val filter = FileSizeFilter(
            minSizeBytes = 1 * 1024 * 1024, // 1MB min
            maxSizeBytes = 15 * 1024 * 1024 // 15MB max
        )

        assertFalse(filter.isEmpty())
        assertTrue(filter.matches(smallJpeg.size)) // 1MB in range
        assertTrue(filter.matches(largeJpeg.size)) // 10MB in range
        assertTrue(filter.matches(smallPng.size)) // 2MB in range
        assertFalse(filter.matches(hugeRaw.size)) // 100MB > max
        assertFalse(filter.matches(tinyBmp.size)) // 500KB < min
    }

    @Test
    fun `FileSizeFilter fromMB conversion edge cases`() {
        // Test with null values (no limits)
        val noLimits = FileSizeFilter.fromMB(null, null)
        assertTrue(noLimits.isEmpty())
        assertEquals(null, noLimits.minSizeBytes)
        assertEquals(null, noLimits.maxSizeBytes)

        // Test with zero min (should be treated as no minimum)
        val zeroMin = FileSizeFilter.fromMB(0f, 10f)
        assertEquals(0L, zeroMin.minSizeBytes)
        assertEquals(10 * 1024 * 1024L, zeroMin.maxSizeBytes)

        // Test with very large max
        val hugeMax = FileSizeFilter.fromMB(1f, 1000f)
        assertEquals(1 * 1024 * 1024L, hugeMax.minSizeBytes)
        assertEquals(1000 * 1024 * 1024L, hugeMax.maxSizeBytes)
    }

    @Test
    fun `FileSizeFilter toMB conversion`() {
        assertEquals(null, FileSizeFilter.toMB(null))
        assertEquals(1f, FileSizeFilter.toMB(1024 * 1024))
        assertEquals(0.5f, FileSizeFilter.toMB(512 * 1024))
        assertEquals(10f, FileSizeFilter.toMB(10 * 1024 * 1024))
    }

    @Test
    fun `FileTypeFilter EMPTY matches all extensions`() {
        val filter = FileTypeFilter.EMPTY

        assertTrue(filter.isEmpty())
        assertTrue(filter.matches("jpg"))
        assertTrue(filter.matches("png"))
        assertTrue(filter.matches("raw"))
        assertTrue(filter.matches("bmp"))
        assertTrue(filter.matches("JPEG")) // Case insensitive
        assertTrue(filter.matches("WeIrD_eXt"))
    }

    @Test
    fun `FileTypeFilter with specific extensions`() {
        val filter = FileTypeFilter(setOf("jpg", "jpeg", "png"))

        assertFalse(filter.isEmpty())
        assertTrue(filter.matches("jpg"))
        assertTrue(filter.matches("JPG")) // Case insensitive
        assertTrue(filter.matches("jpeg"))
        assertTrue(filter.matches("JPEG"))
        assertTrue(filter.matches("png"))
        assertTrue(filter.matches("PNG"))
        assertFalse(filter.matches("raw"))
        assertFalse(filter.matches("bmp"))
        assertFalse(filter.matches("gif"))
    }

    @Test
    fun `FileTypeFilter predefined filters work correctly`() {
        val commonFilter = FileTypeFilter.COMMON_FORMATS
        assertTrue(commonFilter.matches("jpg"))
        assertTrue(commonFilter.matches("jpeg"))
        assertTrue(commonFilter.matches("png"))
        assertFalse(commonFilter.matches("raw"))

        val rawFilter = FileTypeFilter.RAW_FORMATS
        assertTrue(rawFilter.matches("raw"))
        assertTrue(rawFilter.matches("cr2"))
        assertTrue(rawFilter.matches("nef"))
        assertFalse(rawFilter.matches("jpg"))

        val allImagesFilter = FileTypeFilter.ALL_IMAGES
        assertTrue(allImagesFilter.matches("jpg"))
        assertTrue(allImagesFilter.matches("png"))
        assertTrue(allImagesFilter.matches("gif"))
        assertTrue(allImagesFilter.matches("bmp"))
    }

    @Test
    fun `PhotoFilters EMPTY matches all photos`() {
        val filter = PhotoFilters.EMPTY

        assertTrue(filter.isEmpty())
        assertTrue(filter.matches(smallJpeg))
        assertTrue(filter.matches(largeJpeg))
        assertTrue(filter.matches(smallPng))
        assertTrue(filter.matches(hugeRaw))
        assertTrue(filter.matches(tinyBmp))
    }

    @Test
    fun `PhotoFilters with size filter only`() {
        val filter = PhotoFilters(
            fileSizeFilter = FileSizeFilter(
                minSizeBytes = 1 * 1024 * 1024, // 1MB min
                maxSizeBytes = 5 * 1024 * 1024  // 5MB max
            )
        )

        assertFalse(filter.isEmpty())
        assertTrue(filter.matches(smallJpeg))  // 1MB - in range
        assertFalse(filter.matches(largeJpeg)) // 10MB - too large
        assertTrue(filter.matches(smallPng))   // 2MB - in range
        assertFalse(filter.matches(hugeRaw))   // 100MB - too large
        assertFalse(filter.matches(tinyBmp))   // 500KB - too small
    }

    @Test
    fun `PhotoFilters with file type filter only`() {
        val filter = PhotoFilters(
            fileTypeFilter = FileTypeFilter(setOf("jpg", "png"))
        )

        assertFalse(filter.isEmpty())
        assertTrue(filter.matches(smallJpeg))  // jpg - matches
        assertTrue(filter.matches(largeJpeg))  // jpg - matches
        assertTrue(filter.matches(smallPng))   // png - matches
        assertFalse(filter.matches(hugeRaw))   // raw - doesn't match
        assertFalse(filter.matches(tinyBmp))   // bmp - doesn't match
    }

    @Test
    fun `PhotoFilters with both size and type filters`() {
        val filter = PhotoFilters(
            fileSizeFilter = FileSizeFilter(
                minSizeBytes = 1 * 1024 * 1024, // 1MB min
                maxSizeBytes = 50 * 1024 * 1024 // 50MB max
            ),
            fileTypeFilter = FileTypeFilter(setOf("jpg", "raw"))
        )

        assertFalse(filter.isEmpty())
        assertTrue(filter.matches(smallJpeg))  // 1MB jpg - matches both
        assertTrue(filter.matches(largeJpeg))  // 10MB jpg - matches both
        assertFalse(filter.matches(smallPng))  // 2MB png - wrong type
        assertFalse(filter.matches(hugeRaw))   // 100MB raw - too large (even though type matches)
        assertFalse(filter.matches(tinyBmp))   // 500KB bmp - both fail
    }

    @Test
    fun `PhotoFilters edge case - very restrictive filters`() {
        val filter = PhotoFilters(
            fileSizeFilter = FileSizeFilter(
                minSizeBytes = 50 * 1024 * 1024, // 50MB min
                maxSizeBytes = 75 * 1024 * 1024  // 75MB max
            ),
            fileTypeFilter = FileTypeFilter(setOf("xyz")) // Non-existent format
        )

        // Should match nothing
        assertFalse(filter.matches(smallJpeg))
        assertFalse(filter.matches(largeJpeg))
        assertFalse(filter.matches(smallPng))
        assertFalse(filter.matches(hugeRaw))
        assertFalse(filter.matches(tinyBmp))
    }

    @Test
    fun `PhotoFilters edge case - very permissive filters`() {
        val filter = PhotoFilters(
            fileSizeFilter = FileSizeFilter(
                minSizeBytes = null, // No minimum
                maxSizeBytes = null  // No maximum
            ),
            fileTypeFilter = FileTypeFilter.ALL_IMAGES
        )

        // Should match all image files regardless of size
        assertTrue(filter.matches(smallJpeg))
        assertTrue(filter.matches(largeJpeg))
        assertTrue(filter.matches(smallPng))
        assertTrue(filter.matches(tinyBmp))

        // RAW might not be in ALL_IMAGES depending on implementation
        // This tests the actual behavior
        val rawMatches = filter.matches(hugeRaw)
        // We can verify what the actual behavior is
        val allImagesHasRaw = FileTypeFilter.ALL_IMAGES.matches("raw")
        assertEquals(allImagesHasRaw, rawMatches)
    }
}