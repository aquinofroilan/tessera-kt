package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.BillPayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BillPaymentRepository : JpaRepository<BillPayment, java.util.UUID> {
    fun findByBillIdAndOrganizationId(
        billId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<BillPayment>

    fun findByOrganizationId(organizationId: java.util.UUID): List<BillPayment>
}
