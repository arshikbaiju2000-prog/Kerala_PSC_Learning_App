package com.keralapsc.prep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseSigningValidationTest {

    data class ValidationResult(val isValid: Boolean, val error: String? = null)

    private fun validateSigningCredentials(
        keystorePath: String?,
        keystorePassword: String?,
        keyAlias: String?,
        keyPassword: String?
    ): ValidationResult {
        val missing = mutableListOf<String>()
        if (keystorePath.isNullOrBlank()) missing.add("PSC_RELEASE_KEYSTORE_PATH")
        if (keystorePassword.isNullOrBlank()) missing.add("PSC_RELEASE_KEYSTORE_PASSWORD")
        if (keyAlias.isNullOrBlank()) missing.add("PSC_RELEASE_KEY_ALIAS")
        if (keyPassword.isNullOrBlank()) missing.add("PSC_RELEASE_KEY_PASSWORD")

        if (missing.isNotEmpty()) {
            return ValidationResult(
                isValid = false,
                error = "RELEASE SIGNING ERROR: Missing required release signing credentials: ${missing.joinToString(", ")}."
            )
        }

        val keystoreFile = File(keystorePath!!)
        if (!keystoreFile.exists()) {
            return ValidationResult(
                isValid = false,
                error = "RELEASE SIGNING ERROR: Specified keystore file does not exist at path: $keystorePath"
            )
        }

        return ValidationResult(isValid = true)
    }

    @Test
    fun noCredentials_validationFails() {
        val result = validateSigningCredentials(null, null, null, null)
        assertFalse(result.isValid)
        assertTrue(result.error!!.contains("Missing required release signing credentials"))
        assertTrue(result.error!!.contains("PSC_RELEASE_KEYSTORE_PATH"))
        assertTrue(result.error!!.contains("PSC_RELEASE_KEYSTORE_PASSWORD"))
        assertTrue(result.error!!.contains("PSC_RELEASE_KEY_ALIAS"))
        assertTrue(result.error!!.contains("PSC_RELEASE_KEY_PASSWORD"))
    }

    @Test
    fun keystorePathOnly_validationFails() {
        val result = validateSigningCredentials("/tmp/test.jks", null, null, null)
        assertFalse(result.isValid)
        assertTrue(result.error!!.contains("PSC_RELEASE_KEYSTORE_PASSWORD"))
        assertTrue(result.error!!.contains("PSC_RELEASE_KEY_ALIAS"))
        assertTrue(result.error!!.contains("PSC_RELEASE_KEY_PASSWORD"))
    }

    @Test
    fun partialCredentials_validationFails() {
        val result = validateSigningCredentials("/tmp/test.jks", "password", null, null)
        assertFalse(result.isValid)
        assertTrue(result.error!!.contains("PSC_RELEASE_KEY_ALIAS"))
        assertTrue(result.error!!.contains("PSC_RELEASE_KEY_PASSWORD"))
    }

    @Test
    fun nonExistentKeystoreFile_validationFails() {
        val result = validateSigningCredentials(
            keystorePath = "non_existent_keystore_file_12345.jks",
            keystorePassword = "store_password",
            keyAlias = "test_alias",
            keyPassword = "key_password"
        )
        assertFalse(result.isValid)
        assertTrue(result.error!!.contains("Specified keystore file does not exist"))
    }

    @Test
    fun completeValidCredentials_withExistingFile_validationSucceeds() {
        val tempKeystore = File.createTempFile("test_keystore", ".jks")
        try {
            val result = validateSigningCredentials(
                keystorePath = tempKeystore.absolutePath,
                keystorePassword = "store_password",
                keyAlias = "test_alias",
                keyPassword = "key_password"
            )
            assertTrue(result.isValid)
            assertEquals(null, result.error)
        } finally {
            tempKeystore.delete()
        }
    }
}
