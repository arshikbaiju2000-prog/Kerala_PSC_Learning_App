package com.keralapsc.prep.domain.model.content

/**
 * Result of content pack verification prior to installation.
 */
data class ContentPackValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    companion object {
        fun success(warnings: List<String> = emptyList()) = ContentPackValidationResult(
            isValid = true,
            errors = emptyList(),
            warnings = warnings
        )

        fun failure(errors: List<String>, warnings: List<String> = emptyList()) = ContentPackValidationResult(
            isValid = false,
            errors = errors,
            warnings = warnings
        )

        fun failure(error: String) = ContentPackValidationResult(
            isValid = false,
            errors = listOf(error),
            warnings = emptyList()
        )
    }
}
