@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.limanphotos.limandoc.data.search

import com.limanphotos.limandoc.domain.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BoostQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.PhraseQuery
import org.apache.lucene.search.WildcardQuery
import org.apache.lucene.store.FSDirectory
import java.io.File
import java.nio.file.Paths
import kotlin.time.Instant

actual class SearchEngine {

    private var indexWriter: IndexWriter? = null
    private var analyzer = EnglishAnalyzer()
    private var directory: FSDirectory? = null
    private val json = Json { ignoreUnknownKeys = true }

    // Field names for Lucene documents
    companion object {
        private const val FIELD_PHOTO_PATH = "photoPath"
        private const val FIELD_PHOTO_NAME = "photoName"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_TAGS = "tags"
        private const val FIELD_PHOTO_JSON = "photoJson"
        private const val FIELD_INDEXED_AT = "indexedAt"
        private const val FIELD_FILE_SIZE = "fileSize"
        private const val FIELD_CREATION_TIME = "creationTime"
    }

    actual suspend fun initialize(indexDirectory: String) = withContext(Dispatchers.IO) {
        try {
            val indexPath = Paths.get(indexDirectory)
            val indexDir = File(indexDirectory)

            // Create directory if it doesn't exist
            if (!indexDir.exists()) {
                indexDir.mkdirs()
            }

            directory = FSDirectory.open(indexPath)

            val config = IndexWriterConfig(analyzer).apply {
                // Configure for optimal search performance
                // Use CREATE to avoid version compatibility issues with old indexes
                setOpenMode(IndexWriterConfig.OpenMode.CREATE)
                setRAMBufferSizeMB(256.0) // Use 256MB RAM buffer
            }

            indexWriter = IndexWriter(directory, config)

            println("🔍 Search engine initialized with index directory: $indexDirectory")

        } catch (e: Exception) {
            println("❌ SearchEngine initialization error: ${e.message}")

            // Check if it's a codec compatibility issue
            if (e.message?.contains("Lucene95") == true || e.message?.contains("codec") == true) {
                println("🧹 Detected codec compatibility issue - clearing old index")
                try {
                    // Close any open directory
                    directory?.close()

                    // Clear the index directory
                    val indexDir = File(indexDirectory)
                    if (indexDir.exists()) {
                        indexDir.listFiles()?.forEach { file ->
                            file.delete()
                            println("🗑️ Deleted old index file: ${file.name}")
                        }
                        println("🧹 Cleared old index directory")
                    }

                    // Retry initialization with clean directory
                    directory = FSDirectory.open(Paths.get(indexDirectory))
                    val config = IndexWriterConfig(analyzer).apply {
                        setOpenMode(IndexWriterConfig.OpenMode.CREATE)
                        setRAMBufferSizeMB(256.0)
                    }
                    indexWriter = IndexWriter(directory, config)
                    println("✅ Search engine initialized successfully after clearing old index")

                } catch (retryException: Exception) {
                    println("❌ Failed to recover from codec error: ${retryException.message}")
                    throw RuntimeException(
                        "Failed to initialize search engine after clearing index: ${retryException.message}",
                        retryException
                    )
                }
            } else {
                println("❌ Stack trace: ${e.stackTraceToString()}")
                throw RuntimeException("Failed to initialize search engine: ${e.message}", e)
            }
        }
    }

    actual suspend fun indexPhoto(document: SearchDocument) = withContext(Dispatchers.IO) {
        try {
            val luceneDoc = Document().apply {
                // Store photo path as identifier
                add(StringField(FIELD_PHOTO_PATH, document.photo.path, Field.Store.YES))

                // Store searchable photo name
                add(TextField(FIELD_PHOTO_NAME, document.photo.name, Field.Store.YES))

                // Store and index the description for full-text search
                add(TextField(FIELD_DESCRIPTION, document.description, Field.Store.YES))

                // Store and index tags as a single searchable field
                val tagsText = document.tags.joinToString(" ")
                add(TextField(FIELD_TAGS, tagsText, Field.Store.YES))

                // Store complete photo object as JSON for reconstruction
                val photoJson = json.encodeToString(document.photo)
                add(StoredField(FIELD_PHOTO_JSON, photoJson))

                // Store metadata for filtering and sorting
                add(LongPoint(FIELD_INDEXED_AT, document.indexedAt.toEpochMilliseconds()))
                add(StoredField(FIELD_INDEXED_AT, document.indexedAt.toEpochMilliseconds()))

                add(LongPoint(FIELD_FILE_SIZE, document.photo.size))
                add(StoredField(FIELD_FILE_SIZE, document.photo.size))

                add(
                    LongPoint(
                        FIELD_CREATION_TIME,
                        document.photo.creationTime.toEpochMilliseconds()
                    )
                )
                add(
                    StoredField(
                        FIELD_CREATION_TIME,
                        document.photo.creationTime.toEpochMilliseconds()
                    )
                )
            }

            // Remove existing document with same path before adding new one
            indexWriter?.deleteDocuments(Term(FIELD_PHOTO_PATH, document.photo.path))
            indexWriter?.addDocument(luceneDoc)
            indexWriter?.commit()

            println(
                "📝 Indexed photo: ${document.photo.name} - Description: ${
                    document.description.take(
                        100
                    )
                }..."
            )

        } catch (e: Exception) {
            println("❌ Failed to index photo ${document.photo.path}: ${e.message}")
            throw RuntimeException("Failed to index photo", e)
        }
    }

    actual suspend fun search(query: String, limit: Int): List<SearchResult> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                return@withContext emptyList()
            }

            try {
                val reader = DirectoryReader.open(directory)
                val searcher = IndexSearcher(reader)

                // Check if query contains quoted phrases
                val hasQuotedPhrases = query.contains('"') || query.contains("'")

                // Strategy 1: Try phrase search first if quotes detected
                if (hasQuotedPhrases) {
                    val phraseResults =
                        searchWithStrategy(searcher, query, limit, SearchStrategy.PHRASE)
                    if (phraseResults.isNotEmpty()) {
                        reader.close()
                        println("🔍 Phrase search for '$query' returned ${phraseResults.size} results")
                        return@withContext phraseResults
                    }
                }

                // Strategy 2: Try exact match (with stemming)
                val exactResults = searchWithStrategy(searcher, query, limit, SearchStrategy.EXACT)
                if (exactResults.isNotEmpty()) {
                    reader.close()
                    println("🔍 Exact search for '$query' returned ${exactResults.size} results")
                    return@withContext exactResults
                }

                // Strategy 3: Try wildcard search (partial word matching)
                val wildcardResults =
                    searchWithStrategy(searcher, query, limit, SearchStrategy.WILDCARD)
                if (wildcardResults.isNotEmpty()) {
                    reader.close()
                    println("🔍 Wildcard search for '$query' returned ${wildcardResults.size} results")
                    return@withContext wildcardResults
                }

                // Strategy 4: Try fuzzy search (typos and variations, enhanced for longer phrases)
                val fuzzyResults = searchWithStrategy(searcher, query, limit, SearchStrategy.FUZZY)
                reader.close()

                val strategy = if (fuzzyResults.isNotEmpty()) "Fuzzy" else "No"
                println("🔍 $strategy search for '$query' returned ${fuzzyResults.size} results")

                fuzzyResults

            } catch (e: Exception) {
                println("❌ Search failed for query '$query': ${e.message}")
                emptyList()
            }
        }

    private enum class SearchStrategy {
        EXACT,      // Standard query parser with stemming
        PHRASE,     // Exact phrase matching with quotes
        WILDCARD,   // Add wildcards for partial matching
        FUZZY       // Fuzzy matching for typos and longer phrases
    }

    private suspend fun searchWithStrategy(
        searcher: IndexSearcher,
        query: String,
        limit: Int,
        strategy: SearchStrategy
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val booleanQuery = BooleanQuery.Builder()
            val processedQuery = processQuery(query, strategy)

            // Search across all fields with different boost values
            addFieldQuery(
                booleanQuery,
                FIELD_DESCRIPTION,
                processedQuery,
                strategy,
                3.0f
            ) // Highest priority
            addFieldQuery(
                booleanQuery,
                FIELD_TAGS,
                processedQuery,
                strategy,
                2.0f
            )        // Medium priority
            addFieldQuery(
                booleanQuery,
                FIELD_PHOTO_NAME,
                processedQuery,
                strategy,
                1.0f
            )  // Lower priority

            val finalQuery = booleanQuery.build()
            val topDocs = searcher.search(finalQuery, limit)

            topDocs.scoreDocs.map { scoreDoc ->
                val doc = searcher.storedFields().document(scoreDoc.doc)
                val photoJson = doc.get(FIELD_PHOTO_JSON) ?: ""
                val photo = json.decodeFromString<Photo>(photoJson)
                val description = doc.get(FIELD_DESCRIPTION) ?: ""

                // Find which part of the text matched
                val matchedText = findMatchedText(query, description, doc.get(FIELD_TAGS) ?: "")

                SearchResult(
                    photo = photo,
                    score = scoreDoc.score,
                    matchedText = matchedText
                )
            }

        } catch (e: Exception) {
            println("❌ Strategy $strategy failed for '$query': ${e.message}")
            emptyList()
        }
    }

    private fun processQuery(query: String, strategy: SearchStrategy): String {
        return when (strategy) {
            SearchStrategy.EXACT -> query.trim()

            SearchStrategy.PHRASE -> {
                // Extract quoted phrases and preserve them as-is
                extractQuotedPhrases(query.trim())
            }

            SearchStrategy.WILDCARD -> {
                // Add wildcards to words shorter than 6 characters, skip quoted phrases
                val cleanQuery = removeQuotes(query.trim())
                cleanQuery.split("\\s+".toRegex()).joinToString(" ") { word ->
                    if (word.length < 6 && !word.contains("*")) {
                        "$word*"
                    } else {
                        word
                    }
                }
            }

            SearchStrategy.FUZZY -> {
                // Enhanced fuzzy matching for longer phrases with better typo tolerance
                val cleanQuery = removeQuotes(query.trim())
                val words = cleanQuery.split("\\s+".toRegex())

                if (words.size > 2) {
                    // For longer phrases, use more tolerant fuzzy matching
                    words.joinToString(" ") { word ->
                        when {
                            word.length <= 3 -> word  // Short words exact
                            word.length <= 6 -> "$word~1"  // Medium words: 1 edit distance
                            else -> "$word~2"  // Long words: 2 edit distance
                        }
                    }
                } else {
                    // For short queries, use standard fuzzy matching
                    words.joinToString(" ") { word ->
                        if (word.length > 2) "$word~" else word
                    }
                }
            }
        }
    }

    private fun extractQuotedPhrases(query: String): String {
        // Handle both double and single quotes
        val doubleQuotePattern = "\"([^\"]+)\"".toRegex()
        val singleQuotePattern = "'([^']+)'".toRegex()

        var result = query

        // Replace quoted phrases with phrase query syntax
        result = doubleQuotePattern.replace(result) { matchResult ->
            "\"${matchResult.groupValues[1]}\""
        }

        result = singleQuotePattern.replace(result) { matchResult ->
            "\"${matchResult.groupValues[1]}\""
        }

        return result
    }

    private fun removeQuotes(query: String): String {
        return query.replace("\"", "").replace("'", "")
    }

    private fun buildPhraseQueries(
        query: String,
        field: String,
        boost: Float,
        booleanQuery: BooleanQuery.Builder
    ) {
        val doubleQuotePattern = "\"([^\"]+)\"".toRegex()
        val singleQuotePattern = "'([^']+)'".toRegex()

        var foundPhrases = false

        // Handle double quoted phrases
        doubleQuotePattern.findAll(query).forEach { match ->
            val phrase = match.groupValues[1].trim()
            if (phrase.isNotEmpty()) {
                val phraseQuery = buildSinglePhraseQuery(phrase, field)
                if (phraseQuery != null) {
                    val boostedQuery =
                        BoostQuery(phraseQuery, boost * 1.5f) // Higher boost for exact phrases
                    booleanQuery.add(boostedQuery, BooleanClause.Occur.SHOULD)
                    foundPhrases = true
                }
            }
        }

        // Handle single quoted phrases
        singleQuotePattern.findAll(query).forEach { match ->
            val phrase = match.groupValues[1].trim()
            if (phrase.isNotEmpty()) {
                val phraseQuery = buildSinglePhraseQuery(phrase, field)
                if (phraseQuery != null) {
                    val boostedQuery =
                        BoostQuery(phraseQuery, boost * 1.5f) // Higher boost for exact phrases
                    booleanQuery.add(boostedQuery, BooleanClause.Occur.SHOULD)
                    foundPhrases = true
                }
            }
        }

        // Also handle any remaining non-quoted words
        val cleanQuery = query.replace("\"[^\"]*\"".toRegex(), "")
            .replace("'[^']*'".toRegex(), "")
            .trim()

        if (cleanQuery.isNotEmpty() && foundPhrases) {
            try {
                val parser = QueryParser(field, analyzer)
                val remainingQuery = parser.parse(cleanQuery)
                val boostedQuery =
                    BoostQuery(remainingQuery, boost * 0.7f) // Lower boost for non-phrase parts
                booleanQuery.add(boostedQuery, BooleanClause.Occur.SHOULD)
            } catch (e: Exception) {
                println("⚠️ Failed to parse remaining query '$cleanQuery' for field '$field': ${e.message}")
            }
        }
    }

    private fun buildSinglePhraseQuery(phrase: String, field: String): PhraseQuery? {
        return try {
            val terms = phrase.lowercase().split("\\s+".toRegex())
                .filter { it.isNotEmpty() }
                .map { word ->
                    // Apply the analyzer to get stemmed terms
                    val tokenStream = analyzer.tokenStream(field, word)
                    tokenStream.reset()
                    val termAttribute =
                        tokenStream.getAttribute(org.apache.lucene.analysis.tokenattributes.CharTermAttribute::class.java)

                    val analyzedTerms = mutableListOf<String>()
                    while (tokenStream.incrementToken()) {
                        analyzedTerms.add(termAttribute.toString())
                    }
                    tokenStream.close()

                    if (analyzedTerms.isNotEmpty()) analyzedTerms[0] else word
                }

            if (terms.isNotEmpty()) {
                val phraseQueryBuilder = PhraseQuery.Builder()
                terms.forEachIndexed { index, term ->
                    phraseQueryBuilder.add(org.apache.lucene.index.Term(field, term), index)
                }
                phraseQueryBuilder.build()
            } else {
                null
            }
        } catch (e: Exception) {
            println("⚠️ Failed to build phrase query for '$phrase' in field '$field': ${e.message}")
            null
        }
    }

    private fun addFieldQuery(
        booleanQuery: BooleanQuery.Builder,
        field: String,
        query: String,
        strategy: SearchStrategy,
        boost: Float
    ) {
        try {
            when (strategy) {
                SearchStrategy.EXACT -> {
                    val parser = QueryParser(field, analyzer)
                    val fieldQuery = parser.parse(query)
                    val boostedQuery = BoostQuery(fieldQuery, boost)
                    booleanQuery.add(boostedQuery, BooleanClause.Occur.SHOULD)
                }

                SearchStrategy.PHRASE -> {
                    // Build phrase queries for quoted text
                    buildPhraseQueries(query, field, boost, booleanQuery)
                }

                SearchStrategy.WILDCARD, SearchStrategy.FUZZY -> {
                    val parser = QueryParser(field, analyzer)
                    parser.allowLeadingWildcard = true
                    val fieldQuery = parser.parse(query)
                    val boostedQuery = BoostQuery(fieldQuery, boost)
                    booleanQuery.add(boostedQuery, BooleanClause.Occur.SHOULD)
                }
            }
        } catch (e: Exception) {
            // If parsing fails, skip this field
            println("⚠️ Failed to parse query '$query' for field '$field': ${e.message}")
        }
    }

    actual suspend fun removePhoto(photoPath: String) = withContext(Dispatchers.IO) {
        try {
            indexWriter?.deleteDocuments(org.apache.lucene.index.Term(FIELD_PHOTO_PATH, photoPath))
            indexWriter?.commit()
            println("🗑️ Removed photo from index: $photoPath")
        } catch (e: Exception) {
            println("❌ Failed to remove photo from index: ${e.message}")
        }
    }

    suspend fun removePhotosFromFolder(folderPath: String) = withContext(Dispatchers.IO) {
        try {
            // Create a wildcard query to match all photos that start with the folder path
            val normalizedFolderPath = folderPath.replace("\\", "/")
            val wildcardPath = if (normalizedFolderPath.endsWith("/")) {
                "${normalizedFolderPath}*"
            } else {
                "${normalizedFolderPath}/*"
            }

            val wildcardQuery = WildcardQuery(Term(FIELD_PHOTO_PATH, wildcardPath))
            indexWriter?.deleteDocuments(wildcardQuery)
            indexWriter?.commit()
            println("🗑️ Removed photos from folder: $folderPath")
        } catch (e: Exception) {
            println("❌ Failed to remove photos from folder: ${e.message}")
        }
    }

    actual suspend fun clearIndex() = withContext(Dispatchers.IO) {
        try {
            indexWriter?.deleteAll()
            indexWriter?.commit()
            // Force flush to ensure changes are written to disk
            indexWriter?.forceMerge(1)
            indexWriter?.commit()
            println("🧹 Cleared search index and flushed to disk")
        } catch (e: Exception) {
            println("❌ Failed to clear index: ${e.message}")
        }
    }

    actual suspend fun getIndexStats(): IndexStats = withContext(Dispatchers.IO) {
        try {
            // Force any pending changes to be committed first
            indexWriter?.commit()

            val reader = DirectoryReader.open(directory)
            val totalDocs = reader.numDocs()
            reader.close()

            // Calculate index size by examining all files in the directory
            val indexSize = directory?.listAll()?.sumOf { fileName ->
                try {
                    directory?.fileLength(fileName) ?: 0L
                } catch (e: Exception) {
                    0L
                }
            } ?: 0L

            println("📊 Index stats: $totalDocs documents, ${indexSize / 1024 / 1024} MB")

            IndexStats(
                totalDocuments = totalDocs,
                indexSizeBytes = indexSize,
                lastUpdated = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            )

        } catch (e: Exception) {
            println("❌ Failed to get index stats: ${e.message}")
            IndexStats(
                totalDocuments = 0,
                indexSizeBytes = 0L,
                lastUpdated = null
            )
        }
    }

    actual suspend fun close() = withContext(Dispatchers.IO) {
        try {
            indexWriter?.close()
            directory?.close()
            analyzer.close()
            println("🔒 Search engine closed")
        } catch (e: Exception) {
            println("❌ Error closing search engine: ${e.message}")
        }
    }

    /**
     * Find which part of the text matched the query for highlighting
     */
    private fun findMatchedText(query: String, description: String, tags: String): String? {
        val queryWords = query.lowercase().split("\\s+".toRegex())
        val descriptionLower = description.lowercase()
        val tagsLower = tags.lowercase()

        // Look for matches in description first
        for (word in queryWords) {
            if (word.length > 2) { // Ignore very short words
                val index = descriptionLower.indexOf(word)
                if (index >= 0) {
                    // Return a snippet around the match
                    val start = maxOf(0, index - 20)
                    val end = minOf(description.length, index + word.length + 20)
                    return "..." + description.substring(start, end) + "..."
                }
            }
        }

        // Look for matches in tags
        for (word in queryWords) {
            if (word.length > 2 && tagsLower.contains(word)) {
                return "Tags: $tags"
            }
        }

        return null
    }
}