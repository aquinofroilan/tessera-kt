package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.BankStatement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BankStatementRepository : JpaRepository<BankStatement, String> {
    fun findByOrganizationId(organizationId: String): List<BankStatement>

    fun findByOrganizationIdAndBankAccountIdOrderByStatementDateDesc(
        organizationId: String,
        bankAccountId: String,
    ): List<BankStatement>
}
