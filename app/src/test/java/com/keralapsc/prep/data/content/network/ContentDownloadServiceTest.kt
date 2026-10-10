package com.keralapsc.prep.data.content.network

import com.keralapsc.prep.data.content.ContentPackTestHelper
import com.keralapsc.prep.domain.model.content.RemoteContentCatalog
import com.keralapsc.prep.domain.model.content.RemoteContentPackEntry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.util.concurrent.Executors

class ContentDownloadServiceTest {

    private lateinit var server: SimpleHttpTestServer
    private lateinit var downloadService: HttpContentDownloadService
    private lateinit var tempDir: File

    private class SimpleHttpTestServer : AutoCloseable {
        private val serverSocket = ServerSocket(0)
        val port: Int = serverSocket.localPort
        private val executor = Executors.newCachedThreadPool()
        private val handlers = java.util.concurrent.ConcurrentHashMap<String, (OutputStream) -> Unit>()
        @Volatile private var running = true

        init {
            executor.submit {
                while (running) {
                    try {
                        val socket = serverSocket.accept()
                        executor.submit {
                            try {
                                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                                val line = reader.readLine() ?: ""
                                val path = line.split(" ").getOrNull(1) ?: "/"
                                val out = socket.getOutputStream()
                                val handler = handlers[path]
                                if (handler != null) {
                                    handler(out)
                                } else {
                                    out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".toByteArray())
                                }
                                out.flush()
                                socket.close()
                            } catch (e: Exception) {
                                // Socket closed
                            }
                        }
                    } catch (e: Exception) {
                        if (!running) break
                    }
                }
            }
        }

        fun handle(path: String, onResponse: (OutputStream) -> Unit) {
            handlers[path] = onResponse
        }

        override fun close() {
            running = false
            try { serverSocket.close() } catch (e: Exception) {}
            executor.shutdownNow()
        }
    }

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "kpsc_test_${System.currentTimeMillis()}").apply { mkdirs() }
        downloadService = HttpContentDownloadService()
        server = SimpleHttpTestServer()
    }

    @After
    fun tearDown() {
        server.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun fetchCatalog_success_decodesValidCatalog() = runBlocking {
        val testCatalog = RemoteContentCatalog(
            catalogVersion = 1,
            generatedAt = 123456789L,
            packs = listOf(
                RemoteContentPackEntry(
                    packId = "kpsc_10th_exp1",
                    packVersion = 1,
                    formatVersion = 1,
                    minimumAppVersionCode = 1,
                    packName = "10th Level Expansion",
                    description = "500 authentic questions",
                    questionCount = 500,
                    examCount = 2,
                    sizeBytes = 102400L,
                    sha256 = "dummy_sha256",
                    downloadUrl = "http://127.0.0.1:${server.port}/pack.kpsc"
                )
            )
        )

        val jsonStr = Json.encodeToString(testCatalog)
        server.handle("/catalog.json") { out ->
            val bytes = jsonStr.toByteArray()
            val header = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\n\r\n"
            out.write(header.toByteArray())
            out.write(bytes)
        }

        val result = downloadService.fetchCatalog("http://127.0.0.1:${server.port}/catalog.json")
        assertTrue(result.isSuccess)
        val catalog = result.getOrThrow()
        assertEquals(1, catalog.catalogVersion)
        assertEquals(1, catalog.packs.size)
        assertEquals("kpsc_10th_exp1", catalog.packs.first().packId)
    }

    @Test
    fun downloadPack_success_verifiesSha256AndSavesFile() = runBlocking {
        val packBytes = ContentPackTestHelper.buildKpscArchive(packId = "download_test", packVersion = 1)
        val expectedSha = ContentPackTestHelper.computeSha256(packBytes)

        server.handle("/test.kpsc") { out ->
            val header = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: ${packBytes.size}\r\n\r\n"
            out.write(header.toByteArray())
            out.write(packBytes)
        }

        val destFile = File(tempDir, "download_test.kpsc")
        val result = downloadService.downloadPack(
            downloadUrl = "http://127.0.0.1:${server.port}/test.kpsc",
            destinationFile = destFile,
            expectedSha256 = expectedSha
        )

        assertTrue(result.isSuccess)
        assertTrue(destFile.exists())
        assertEquals(packBytes.size.toLong(), destFile.length())
    }

    @Test
    fun downloadPack_shaMismatch_deletesFileAndReturnsFailure() = runBlocking {
        val packBytes = ContentPackTestHelper.buildKpscArchive(packId = "download_test", packVersion = 1)

        server.handle("/test.kpsc") { out ->
            val header = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: ${packBytes.size}\r\n\r\n"
            out.write(header.toByteArray())
            out.write(packBytes)
        }

        val destFile = File(tempDir, "tampered.kpsc")
        val result = downloadService.downloadPack(
            downloadUrl = "http://127.0.0.1:${server.port}/test.kpsc",
            destinationFile = destFile,
            expectedSha256 = "invalid_expected_sha256_hash_value_123456"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        assertFalse("Destination file must be deleted on SHA mismatch", destFile.exists())
    }

    @Test
    fun downloadPack_http404_deletesFileAndReturnsFailure() = runBlocking {
        val destFile = File(tempDir, "missing.kpsc")
        val result = downloadService.downloadPack(
            downloadUrl = "http://127.0.0.1:${server.port}/not_found.kpsc",
            destinationFile = destFile
        )

        assertTrue(result.isFailure)
        assertFalse(destFile.exists())
    }
}
