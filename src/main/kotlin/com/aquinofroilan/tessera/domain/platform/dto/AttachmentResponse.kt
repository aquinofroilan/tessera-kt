package com.aquinofroilan.tessera.domain.platform.dto

import com.aquinofroilan.tessera.domain.platform.model.Attachment

data class AttachmentResponse(
    val id: java.util.UUID,
    val entityType: String,
    val entityId: java.util.UUID,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uploadedBy: java.util.UUID,
) {
    companion object {
        fun from(a: Attachment) =
            AttachmentResponse(
                id = a.id,
                entityType = a.entityType,
                entityId = a.entityId,
                filename = a.filename,
                mimeType = a.mimeType,
                sizeBytes = a.sizeBytes,
                uploadedBy = a.uploadedBy,
            )
    }
}
