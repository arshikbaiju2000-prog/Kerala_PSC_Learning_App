package com.keralapsc.prep.domain.usecase.content

import com.keralapsc.prep.domain.model.content.ContentPackManifest
import com.keralapsc.prep.domain.model.content.ContentPackPayload
import com.keralapsc.prep.domain.model.content.ContentPackValidationResult
import com.keralapsc.prep.domain.repository.ContentPackRepository

class ValidateContentPackUseCase(
    private val repository: ContentPackRepository
) {
    suspend operator fun invoke(
        manifest: ContentPackManifest,
        payload: ContentPackPayload
    ): ContentPackValidationResult {
        return repository.validateContentPack(manifest, payload)
    }
}
