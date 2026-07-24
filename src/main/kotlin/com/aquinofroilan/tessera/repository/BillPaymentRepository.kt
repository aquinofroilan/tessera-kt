package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.BillPayment
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
