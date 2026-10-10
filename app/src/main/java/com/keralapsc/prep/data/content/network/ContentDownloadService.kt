package com.keralapsc.prep.data.content.network

import com.keralapsc.prep.domain.model.content.RemoteContentCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

interface ContentDownloadService {
    suspend fun fetchCatalog(catalogUrl: String): Result<RemoteContentCatalog>

    suspend fun downloadPack(
        downloadUrl: String,
        destinationFile: File,
        expectedSha256: String? = null,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null
    ): Result<File>
}

class HttpContentDownloadService(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000
) : ContentDownloadService {

    override suspend fun fetchCatalog(catalogUrl: String): Result<RemoteContentCatalog> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(catalogUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    instanceFollowRedirects = true
                }

                try {
                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        throw IOException("Server returned HTTP error code $responseCode: ${connection.responseMessage}")
                    }

                    val jsonText = connection.inputStream.use { it.bufferedReader().readText() }
                    json.decodeFromString<RemoteContentCatalog>(jsonText)
                } finally {
                    connection.disconnect()
                }
            }
        }

    override suspend fun downloadPack(
        downloadUrl: String,
        destinationFile: File,
        expectedSha256: String?,
        onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)?
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            // Ensure parent directory exists
            destinationFile.parentFile?.mkdirs()
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                requestMethod = "GET"
                setRequestProperty("Accept", "application/octet-stream")
                instanceFollowRedirects = true
            }

            val digest = MessageDigest.getInstance("SHA-256")

            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IOException("Server returned HTTP error code $responseCode: ${connection.responseMessage}")
                }

                val totalBytes = connection.contentLengthLong

                connection.inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloaded = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            digest.update(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            onProgress?.invoke(downloaded, totalBytes)
                        }
                        output.flush()
                    }
                }

                if (expectedSha256 != null) {
                    val computedSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!computedSha256.equals(expectedSha256.trim(), ignoreCase = true)) {
                        destinationFile.delete()
                        throw SecurityException(
                            "Downloaded archive SHA-256 mismatch. Expected: $expectedSha256, Computed: $computedSha256"
                        )
                    }
                }

                destinationFile
            } catch (e: Exception) {
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                throw e
            } finally {
                connection.disconnect()
            }
        }
    }
}
