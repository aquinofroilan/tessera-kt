package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.Account
import com.aquinofroilan.tessera.domain.finance.model.AccountType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AccountRepository : JpaRepository<Account, java.util.UUID> {
    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<Account>

    fun findByOrganizationIdAndTypeAndIsActive(
        organizationId: java.util.UUID,
        type: AccountType,
        isActive: Boolean,
    ): List<Account>

    fun findByOrganizationIdAndParentIdAndIsActive(
        organizationId: java.util.UUID,
        parentId: java.util.UUID,
        isActive: Boolean,
    ): List<Account>

    fun findByOrganizationIdAndTypeAndParentIdAndIsActive(
        organizationId: java.util.UUID,
        type: AccountType,
        parentId: java.util.UUID,
        isActive: Boolean,
    ): List<Account>

    fun findByOrganizationIdAndCode(
        organizationId: java.util.UUID,
        code: String,
    ): Optional<Account>

    fun existsByOrganizationIdAndCode(
        organizationId: java.util.UUID,
        code: String,
    ): Boolean

    fun existsByOrganizationIdAndParentId(
        organizationId: java.util.UUID,
        parentId: java.util.UUID,
    ): Boolean

    fun existsByOrganizationIdAndParentIdAndIsActive(
        organizationId: java.util.UUID,
        parentId: java.util.UUID,
        isActive: Boolean,
    ): Boolean
}
