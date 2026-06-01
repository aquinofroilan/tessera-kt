package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Attachment
import com.aquinofroilan.tessera.repository.AttachmentRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Polymorphic attachment service. Stores file bytes on the local filesystem
 * under `tessera.attachments.dir` (default `./attachments`). The metadata
 * row holds the relative `storage_key`, so swapping the backend later (S3,
 * GCS) only needs a new implementation of [storeBytes]/[openStream]/[deleteBytes].
 */
@Service
class AttachmentService(
    private val attachmentRepository: AttachmentRepository,
    @Value("\${tessera.attachments.dir:./attachments}") private val rootDir: String,
) {
    private val root: Path by lazy {
        val p = Path.of(rootDir).toAbsolutePath().normalize()
        Files.createDirectories(p)
        p
    }

    @Transactional
    fun upload(
        file: MultipartFile,
        entityType: String,
        entityId: String,
        organizationId: String,
        userId: String,
    ): Attachment {
        if (file.isEmpty) throw BusinessRuleException("Uploaded file is empty")
        if (entityType.isBlank()) throw BusinessRuleException("entityType is required")
        if (entityId.isBlank()) throw BusinessRuleException("entityId is required")
        val filename = sanitiseFilename(file.originalFilename ?: "upload.bin")
        val attachmentId =
            java.util.UUID
                .randomUUID()
                .toString()
        val relativeKey = "$organizationId/$entityType/$entityId/$attachmentId-$filename"
        storeBytes(relativeKey, file)
        return attachmentRepository.save(
            Attachment(
                id = attachmentId,
                organizationId = organizationId,
                entityType = entityType,
                entityId = entityId,
                filename = filename,
                mimeType = file.contentType ?: "application/octet-stream",
                sizeBytes = file.size,
                storageKey = relativeKey,
                uploadedBy = userId,
            ),
        )
    }

    fun getAttachment(
        id: String,
        organizationId: String,
    ): Attachment {
        val a =
            attachmentRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Attachment not found: $id")
            }
        if (a.organizationId != organizationId) {
            throw ResourceNotFoundException("Attachment not found: $id")
        }
        return a
    }

    fun listForEntity(
        organizationId: String,
        entityType: String,
        entityId: String,
    ): List<Attachment> = attachmentRepository.findByOrganizationIdAndEntityTypeAndEntityId(organizationId, entityType, entityId)

    fun openStream(attachment: Attachment): InputStream {
        val path = resolvePath(attachment.storageKey)
        if (!Files.exists(path)) {
            throw ResourceNotFoundException("Stored bytes missing for attachment ${attachment.id}")
        }
        return Files.newInputStream(path)
    }

    @Transactional
    fun delete(
        id: String,
        organizationId: String,
    ) {
        val a = getAttachment(id, organizationId)
        attachmentRepository.delete(a)
        deleteBytes(a.storageKey)
    }

    private fun storeBytes(
        relativeKey: String,
        file: MultipartFile,
    ) {
        val target = resolvePath(relativeKey)
        Files.createDirectories(target.parent)
        file.inputStream.use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun deleteBytes(relativeKey: String) {
        val target = resolvePath(relativeKey)
        Files.deleteIfExists(target)
    }

    private fun resolvePath(relativeKey: String): Path {
        val resolved = root.resolve(relativeKey).normalize()
        if (!resolved.startsWith(root)) {
            throw BusinessRuleException("Attachment path escapes storage root")
        }
        return resolved
    }

    private fun sanitiseFilename(name: String): String {
        val basename = name.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = basename.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.', '_', ' ')
        return cleaned.ifBlank { "upload.bin" }
    }
}
