package com.froilan.synectix.repository

import com.froilan.synectix.model.Organizations
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface OrganizationRepository : MongoRepository<Organizations, String> {
    fun findByOrgSlug(orgSlug: String): Optional<Organizations>

    fun existsByOrgSlug(orgSlug: String): Boolean

    fun existsByName(name: String): Boolean
}
