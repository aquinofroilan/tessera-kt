package com.aquinofroilan.tessera.domain.hr.dto

import com.aquinofroilan.tessera.domain.hr.model.DocumentCategory
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

data class AttachEmployeeDocumentRequest(
    @field:NotNull(message = "Attachment ID is required")
    val attachmentId: UUID?,
    @field:NotNull(message = "Category is required")
    val category: DocumentCategory?,
    val expiryDate: LocalDate? = null,
)
