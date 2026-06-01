package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "attachments")
@EntityListeners(AuditingEntityListener::class)
data class Attachment(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "entity_type")
    val entityType: String,
    @Column(name = "entity_id", columnDefinition = "uuid")
    val entityId: String,
    val filename: String,
    @Column(name = "mime_type")
    val mimeType: String,
    @Column(name = "size_bytes")
    val sizeBytes: Long,
    @Column(name = "storage_key")
    val storageKey: String,
    @Column(name = "uploaded_by", columnDefinition = "uuid")
    val uploadedBy: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
)
