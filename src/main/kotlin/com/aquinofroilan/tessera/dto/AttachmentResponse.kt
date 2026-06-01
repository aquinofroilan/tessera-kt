package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.Attachment

data class AttachmentResponse(
    val id: String,
    val entityType: String,
    val entityId: String,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uploadedBy: String,
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
