package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.BankAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface BankAccountRepository : JpaRepository<BankAccount, String> {
    fun findByOrganizationId(organizationId: String): List<BankAccount>

    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<BankAccount>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<BankAccount>
}
