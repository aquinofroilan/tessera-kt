package com.loom.synectix.repository

import com.loom.synectix.model.Account
import com.loom.synectix.model.AccountType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AccountRepository : JpaRepository<Account, String> {
    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<Account>

    fun findByOrganizationIdAndTypeAndIsActive(
        organizationId: String,
        type: AccountType,
        isActive: Boolean,
    ): List<Account>

    fun findByOrganizationIdAndParentIdAndIsActive(
        organizationId: String,
        parentId: String,
        isActive: Boolean,
    ): List<Account>

    fun findByOrganizationIdAndTypeAndParentIdAndIsActive(
        organizationId: String,
        type: AccountType,
        parentId: String,
        isActive: Boolean,
    ): List<Account>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<Account>

    fun existsByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Boolean

    fun existsByOrganizationIdAndParentId(
        organizationId: String,
        parentId: String,
    ): Boolean

    fun existsByOrganizationIdAndParentIdAndIsActive(
        organizationId: String,
        parentId: String,
        isActive: Boolean,
    ): Boolean
}
