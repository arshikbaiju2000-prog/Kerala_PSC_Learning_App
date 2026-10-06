package com.keralapsc.prep.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationResourceTest {

    private fun extractStringKeys(xmlFile: File): Set<String> {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(xmlFile)
        doc.documentElement.normalize()

        val stringNodes = doc.getElementsByTagName("string")
        val keys = mutableSetOf<String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue
            if (name != null) {
                keys.add(name)
            }
        }
        return keys
    }

    private fun extractPluralKeys(xmlFile: File): Set<String> {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(xmlFile)
        doc.documentElement.normalize()

        val pluralNodes = doc.getElementsByTagName("plurals")
        val keys = mutableSetOf<String>()
        for (i in 0 until pluralNodes.length) {
            val node = pluralNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue
            if (name != null) {
                keys.add(name)
            }
        }
        return keys
    }

    @Test
    fun verifyStringResourceParityBetweenEnglishAndMalayalam() {
        // Resolve res directory from project root or current working dir
        val baseRes = File("src/main/res").takeIf { it.exists() }
            ?: File("app/src/main/res").takeIf { it.exists() }
            ?: throw IllegalStateException("Cannot find src/main/res directory")

        val enStringsFile = File(baseRes, "values/strings.xml")
        val mlStringsFile = File(baseRes, "values-ml/strings.xml")

        assertTrue("values/strings.xml must exist", enStringsFile.exists())
        assertTrue("values-ml/strings.xml must exist", mlStringsFile.exists())

        val enKeys = extractStringKeys(enStringsFile)
        val mlKeys = extractStringKeys(mlStringsFile)

        val missingInMl = enKeys - mlKeys
        val missingInEn = mlKeys - enKeys

        assertTrue(
            "Keys missing in values-ml/strings.xml: $missingInMl",
            missingInMl.isEmpty()
        )
        assertTrue(
            "Keys missing in values/strings.xml: $missingInEn",
            missingInEn.isEmpty()
        )
        assertEquals("English and Malayalam key count must match", enKeys.size, mlKeys.size)
    }

    @Test
    fun verifyPluralResourceParityBetweenEnglishAndMalayalam() {
        val baseRes = File("src/main/res").takeIf { it.exists() }
            ?: File("app/src/main/res").takeIf { it.exists() }
            ?: throw IllegalStateException("Cannot find src/main/res directory")

        val enPluralsFile = File(baseRes, "values/plurals.xml")
        val mlPluralsFile = File(baseRes, "values-ml/plurals.xml")

        assertTrue("values/plurals.xml must exist", enPluralsFile.exists())
        assertTrue("values-ml/plurals.xml must exist", mlPluralsFile.exists())

        val enPluralKeys = extractPluralKeys(enPluralsFile)
        val mlPluralKeys = extractPluralKeys(mlPluralsFile)

        val missingInMl = enPluralKeys - mlPluralKeys
        val missingInEn = mlPluralKeys - enPluralKeys

        assertTrue(
            "Plurals missing in values-ml: $missingInMl",
            missingInMl.isEmpty()
        )
        assertTrue(
            "Plurals missing in values: $missingInEn",
            missingInEn.isEmpty()
        )
    }
}
