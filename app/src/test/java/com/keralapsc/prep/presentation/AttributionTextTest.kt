package com.keralapsc.prep.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AttributionTextTest {

    private fun loadStringResourceMap(xmlFile: File): Map<String, String> {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(xmlFile)
        doc.documentElement.normalize()

        val stringNodes = doc.getElementsByTagName("string")
        val map = mutableMapOf<String, String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue
            val value = node.textContent
            if (name != null && value != null) {
                map[name] = value
            }
        }
        return map
    }

    @Test
    fun verifyRequiredAttributionStatements() {
        val baseRes = File("src/main/res").takeIf { it.exists() }
            ?: File("app/src/main/res").takeIf { it.exists() }
            ?: throw IllegalStateException("Cannot find src/main/res directory")

        val stringsMap = loadStringResourceMap(File(baseRes, "values/strings.xml"))

        val identityStatement = stringsMap["attribution_identity_statement"]
        val disclaimerStatement = stringsMap["attribution_disclaimer_statement"]
        val unavailableMsg = stringsMap["attribution_provenance_unavailable"]

        assertEquals(
            "This is an independent educational preparation application.",
            identityStatement
        )

        assertEquals(
            "This application is not affiliated with, authorized by, sponsored by, or endorsed by the Kerala Public Service Commission (KPSC) or the Government of Kerala.",
            disclaimerStatement
        )

        assertTrue(
            "Fallback message for unavailable provenance must exist",
            !unavailableMsg.isNullOrBlank()
        )

        // Verify no false claims of official authorization, partnership, or certification
        for ((key, value) in stringsMap) {
            if (key.startsWith("attribution_")) {
                val lower = value.lowercase()
                assertFalse("Must not claim official endorsement in $key", lower.contains("officially endorsed"))
                assertFalse("Must not claim official partnership in $key", lower.contains("official partner"))
                assertFalse("Must not claim government authorization in $key", lower.contains("officially authorized"))
            }
        }
    }
}
