package com.aquinofroilan.tessera.repository
import com.aquinofroilan.tessera.model.Contact
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ContactRepository : JpaRepository<Contact, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<Contact>

    fun findByOrganizationIdAndIsActive(
        organizationId: UUID,
        isActive: Boolean,
    ): List<Contact>

    fun findByOrganizationIdAndCustomerId(
        organizationId: UUID,
        customerId: UUID,
    ): List<Contact>
}
