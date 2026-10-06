package com.keralapsc.prep.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ContentPackParserTest {

    private lateinit var parser: ContentPackParser

    @Before
    fun setUp() {
        parser = ContentPackParser()
    }

    @Test
    fun parseValidArchive_succeedsWithCorrectManifestAndPayload() {
        val payload = ContentPackTestHelper.createMinimalValidPayload()
        val archiveBytes = ContentPackTestHelper.buildKpscArchive(
            payload = payload,
            packId = "kpsc_core_syllabus",
            packVersion = 1
        )

        val result = parser.parseArchive(ByteArrayInputStream(archiveBytes))
        assertTrue("Parser should return Success on valid archive", result is ContentPackParseResult.Success)

        val pack = (result as ContentPackParseResult.Success).pack
        assertEquals("kpsc_core_syllabus", pack.manifest.packId)
        assertEquals(1, pack.manifest.packVersion)
        assertEquals(1, pack.payload.questions.size)
        assertEquals("q_test_1", pack.payload.questions[0].id)
    }

    @Test
    fun parseArchiveWithSha256Mismatch_isRejected() {
        val payload = ContentPackTestHelper.createMinimalValidPayload()
        val archiveBytes = ContentPackTestHelper.buildKpscArchive(
            payload = payload,
            packId = "kpsc_tampered",
            packVersion = 1,
            contentSha256Override = "0000000000000000000000000000000000000000000000000000000000000000"
        )

        val result = parser.parseArchive(ByteArrayInputStream(archiveBytes))
        assertTrue("Parser should return Error when SHA-256 does not match", result is ContentPackParseResult.Error)

        val error = result as ContentPackParseResult.Error
        assertTrue(error.message.contains("SHA-256 digest mismatch"))
    }

    @Test
    fun parseArchiveMissingManifest_isRejected() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("content.json"))
            zos.write("{}".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val result = parser.parseArchive(ByteArrayInputStream(baos.toByteArray()))
        assertTrue(result is ContentPackParseResult.Error)
        val error = result as ContentPackParseResult.Error
        assertTrue(error.message.contains("manifest.json"))
    }

    @Test
    fun parseArchiveMissingContent_isRejected() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write("{}".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val result = parser.parseArchive(ByteArrayInputStream(baos.toByteArray()))
        assertTrue(result is ContentPackParseResult.Error)
        val error = result as ContentPackParseResult.Error
        assertTrue(error.message.contains("content.json"))
    }

    @Test
    fun parseCorruptedZip_isRejected() {
        val corruptedBytes = "NOT A VALID ZIP ARCHIVE CONTENT".toByteArray(Charsets.UTF_8)
        val result = parser.parseArchive(ByteArrayInputStream(corruptedBytes))
        assertTrue(result is ContentPackParseResult.Error)
    }
}
