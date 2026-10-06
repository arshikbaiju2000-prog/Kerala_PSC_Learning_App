package com.keralapsc.prep.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ContentPackParserSecurityTest {

    private val parser = ContentPackParser()

    private fun createZipStream(entries: Map<String, ByteArray>): ByteArrayInputStream {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            for ((name, data) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
        return ByteArrayInputStream(bos.toByteArray())
    }

    @Test
    fun parseArchive_rejectsIllegalPathTraversal() {
        val stream = createZipStream(
            mapOf("../secret.txt" to "compromised".toByteArray())
        )
        val result = parser.parseArchive(stream)
        assertTrue(result is ContentPackParseResult.Error)
        val error = result as ContentPackParseResult.Error
        assertTrue(error.message.contains("illegal entry path"))
    }

    @Test
    fun parseArchive_rejectsAbsolutePath() {
        val stream = createZipStream(
            mapOf("/manifest.json" to "{}".toByteArray())
        )
        val result = parser.parseArchive(stream)
        assertTrue(result is ContentPackParseResult.Error)
        val error = result as ContentPackParseResult.Error
        assertTrue(error.message.contains("illegal entry path"))
    }

    @Test
    fun parseArchive_rejectsUnexpectedEntryNames() {
        val stream = createZipStream(
            mapOf("malicious.sh" to "#!/bin/bash".toByteArray())
        )
        val result = parser.parseArchive(stream)
        assertTrue(result is ContentPackParseResult.Error)
        val error = result as ContentPackParseResult.Error
        assertTrue(error.message.contains("Only manifest.json and content.json are permitted"))
    }

    @Test
    fun parseArchive_rejectsExceedingEntryCount() {
        val entries = mutableMapOf<String, ByteArray>()
        for (i in 1..55) {
            entries["dir_$i/"] = ByteArray(0)
        }
        val stream = createZipStream(entries)
        val result = parser.parseArchive(stream)
        assertTrue(result is ContentPackParseResult.Error)
        val error = result as ContentPackParseResult.Error
        assertTrue(error.message.contains("Archive contains too many entries"))
    }

    @Test
    fun parseArchive_rejectsOversizedManifest() {
        // Create a fake zip where manifest.json exceeds 1 MB limit
        val oversizedManifest = ByteArray(1024 * 1024 + 100) { 'A'.code.toByte() }
        val stream = createZipStream(
            mapOf("manifest.json" to oversizedManifest)
        )
        val result = parser.parseArchive(stream)
        assertTrue(result is ContentPackParseResult.Error)
        val error = result as ContentPackParseResult.Error
        assertTrue(error.message.contains("exceeds bounded limit") || error.message.contains("limit"))
    }

    @Test
    fun parseArchive_rejectsWhenMissingRequiredFiles() {
        val stream = createZipStream(
            mapOf("manifest.json" to "{}".toByteArray())
        )
        val result = parser.parseArchive(stream)
        assertTrue(result is ContentPackParseResult.Error)
        val error = result as ContentPackParseResult.Error
        assertTrue(error.message.contains("Archive does not contain content.json"))
    }
}
