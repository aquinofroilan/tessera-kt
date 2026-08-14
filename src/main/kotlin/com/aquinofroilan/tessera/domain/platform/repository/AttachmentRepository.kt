package com.aquinofroilan.tessera.domain.platform.repository

import com.aquinofroilan.tessera.domain.platform.model.Attachment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AttachmentRepository : JpaRepository<Attachment, java.util.UUID> {
    fun findByOrganizationIdAndEntityTypeAndEntityId(
        organizationId: java.util.UUID,
        entityType: String,
        entityId: java.util.UUID,
    ): List<Attachment>

    fun findByOrganizationId(organizationId: java.util.UUID): List<Attachment>
}
