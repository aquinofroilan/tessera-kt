package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Attachment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AttachmentRepository : JpaRepository<Attachment, String> {
    fun findByOrganizationIdAndEntityTypeAndEntityId(
        organizationId: String,
        entityType: String,
        entityId: String,
    ): List<Attachment>

    fun findByOrganizationId(organizationId: String): List<Attachment>
}
