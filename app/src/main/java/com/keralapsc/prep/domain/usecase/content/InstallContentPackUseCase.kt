package com.keralapsc.prep.domain.usecase.content

import com.keralapsc.prep.data.content.ContentPackParseResult
import com.keralapsc.prep.domain.model.content.InstalledContentPack
import com.keralapsc.prep.domain.repository.ContentPackRepository
import java.io.InputStream

class InstallContentPackUseCase(
    private val repository: ContentPackRepository
) {
    suspend operator fun invoke(inputStream: InputStream): Result<InstalledContentPack> {
        val parseResult = repository.parseArchive(inputStream)
        if (parseResult is ContentPackParseResult.Error) {
            return Result.failure(IllegalArgumentException("Archive parsing failed: ${parseResult.message}", parseResult.cause))
        }

        val parsedPack = (parseResult as ContentPackParseResult.Success).pack
        val validation = repository.validateContentPack(parsedPack.manifest, parsedPack.payload)
        if (!validation.isValid) {
            return Result.failure(
                IllegalArgumentException("Content pack validation failed:\n${validation.errors.joinToString("\n")}")
            )
        }

        val installResult = repository.installContentPack(
            manifest = parsedPack.manifest,
            payload = parsedPack.payload,
            checksum = parsedPack.computedSha256
        )

        return installResult.mapCatching {
            repository.getActivePack(parsedPack.manifest.packId)
                ?: throw IllegalStateException("Installed pack could not be loaded after commit")
        }
    }
}
