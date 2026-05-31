package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Organizations
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface OrganizationRepository : JpaRepository<Organizations, String> {
    fun findByOrgSlug(orgSlug: String): Optional<Organizations>

    fun existsByOrgSlug(orgSlug: String): Boolean

    fun existsByName(name: String): Boolean
}
