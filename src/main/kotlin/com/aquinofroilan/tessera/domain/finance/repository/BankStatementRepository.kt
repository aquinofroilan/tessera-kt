package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.BankStatement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BankStatementRepository : JpaRepository<BankStatement, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<BankStatement>

    fun findByOrganizationIdAndBankAccountIdOrderByStatementDateDesc(
        organizationId: java.util.UUID,
        bankAccountId: java.util.UUID,
    ): List<BankStatement>
}
