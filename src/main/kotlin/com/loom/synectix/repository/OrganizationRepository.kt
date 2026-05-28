package com.loom.synectix.repository

import com.loom.synectix.model.Organizations
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface OrganizationRepository : JpaRepository<Organizations, String> {
    fun findByOrgSlug(orgSlug: String): Optional<Organizations>

    fun existsByOrgSlug(orgSlug: String): Boolean

    fun existsByName(name: String): Boolean
}
