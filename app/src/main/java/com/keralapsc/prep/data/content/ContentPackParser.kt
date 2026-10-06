package com.keralapsc.prep.data.content

import com.keralapsc.prep.domain.model.content.ContentPackManifest
import com.keralapsc.prep.domain.model.content.ContentPackPayload
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class ParsedContentPack(
    val manifest: ContentPackManifest,
    val payload: ContentPackPayload,
    val computedSha256: String
)

sealed interface ContentPackParseResult {
    data class Success(val pack: ParsedContentPack) : ContentPackParseResult
    data class Error(val message: String, val cause: Throwable? = null) : ContentPackParseResult
}

class ContentPackParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {

    companion object {
        const val MAX_MANIFEST_BYTES = 1 * 1024 * 1024L // 1 MB
        const val MAX_CONTENT_BYTES = 50 * 1024 * 1024L // 50 MB
        const val MAX_ENTRY_BYTES = 50 * 1024 * 1024L // 50 MB
        const val MAX_TOTAL_ARCHIVE_BYTES = 60 * 1024 * 1024L // 60 MB
        const val MAX_ZIP_ENTRIES = 50

        val ALLOWED_ENTRY_FILENAMES = setOf("manifest.json", "content.json")
    }

    /**
     * Parses a .kpsc archive stream, extracting manifest.json and content.json while
     * verifying the computed SHA-256 hash against manifest.contentSha256.
     * Enforces stream-bounded decompression limits and path safety during reading.
     */
    fun parseArchive(inputStream: InputStream): ContentPackParseResult {
        return try {
            val zipIn = ZipInputStream(inputStream)
            var manifestBytes: ByteArray? = null
            var contentBytes: ByteArray? = null
            var entryCount = 0
            var cumulativeBytes = 0L

            var entry = zipIn.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_ZIP_ENTRIES) {
                    return ContentPackParseResult.Error(
                        "Archive contains too many entries: $entryCount (max allowed: $MAX_ZIP_ENTRIES)"
                    )
                }

                val rawName = entry.name
                // Path safety: reject directory traversal, absolute paths
                if (rawName.contains("..") || rawName.startsWith("/") || rawName.startsWith("\\")) {
                    return ContentPackParseResult.Error(
                        "Archive contains illegal entry path: '$rawName'"
                    )
                }

                // Skip directory entries if any
                if (!entry.isDirectory) {
                    val normalizedName = rawName.substringAfterLast('/')
                    if (normalizedName !in ALLOWED_ENTRY_FILENAMES) {
                        return ContentPackParseResult.Error(
                            "Archive contains unexpected entry: '$rawName'. Only manifest.json and content.json are permitted."
                        )
                    }

                    when (normalizedName) {
                        "manifest.json" -> {
                            val (bytes, readCount) = readEntryBytesBounded(
                                zipIn = zipIn,
                                entryLimit = MAX_MANIFEST_BYTES,
                                currentCumulative = cumulativeBytes,
                                totalLimit = MAX_TOTAL_ARCHIVE_BYTES
                            )
                            manifestBytes = bytes
                            cumulativeBytes += readCount
                        }
                        "content.json" -> {
                            val (bytes, readCount) = readEntryBytesBounded(
                                zipIn = zipIn,
                                entryLimit = MAX_CONTENT_BYTES,
                                currentCumulative = cumulativeBytes,
                                totalLimit = MAX_TOTAL_ARCHIVE_BYTES
                            )
                            contentBytes = bytes
                            cumulativeBytes += readCount
                        }
                    }
                }

                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }

            if (manifestBytes == null) {
                return ContentPackParseResult.Error("Archive does not contain manifest.json")
            }
            if (contentBytes == null) {
                return ContentPackParseResult.Error("Archive does not contain content.json")
            }

            val manifest = try {
                json.decodeFromString<ContentPackManifest>(manifestBytes.decodeToString())
            } catch (e: Exception) {
                return ContentPackParseResult.Error("Failed to parse manifest.json: ${e.message}", e)
            }

            val computedSha256 = computeSha256(contentBytes)
            if (!computedSha256.equals(manifest.contentSha256.trim(), ignoreCase = true)) {
                return ContentPackParseResult.Error(
                    "SHA-256 digest mismatch. Expected: ${manifest.contentSha256}, Computed: $computedSha256"
                )
            }

            val payload = try {
                json.decodeFromString<ContentPackPayload>(contentBytes.decodeToString())
            } catch (e: Exception) {
                return ContentPackParseResult.Error("Failed to parse content.json: ${e.message}", e)
            }

            ContentPackParseResult.Success(
                ParsedContentPack(
                    manifest = manifest,
                    payload = payload,
                    computedSha256 = computedSha256
                )
            )
        } catch (e: Exception) {
            ContentPackParseResult.Error("Failed to read .kpsc archive: ${e.message}", e)
        }
    }

    private fun readEntryBytesBounded(
        zipIn: ZipInputStream,
        entryLimit: Long,
        currentCumulative: Long,
        totalLimit: Long
    ): Pair<ByteArray, Long> {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var totalEntryRead = 0L
        var read: Int

        while (zipIn.read(buffer).also { read = it } != -1) {
            totalEntryRead += read
            if (totalEntryRead > entryLimit) {
                throw IllegalStateException(
                    "Entry exceeded maximum allowed uncompressed size: $totalEntryRead bytes (limit: $entryLimit bytes)"
                )
            }
            if (currentCumulative + totalEntryRead > totalLimit) {
                throw IllegalStateException(
                    "Archive exceeded maximum cumulative uncompressed size: ${currentCumulative + totalEntryRead} bytes (limit: $totalLimit bytes)"
                )
            }
            out.write(buffer, 0, read)
        }
        return Pair(out.toByteArray(), totalEntryRead)
    }

    private fun computeSha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
