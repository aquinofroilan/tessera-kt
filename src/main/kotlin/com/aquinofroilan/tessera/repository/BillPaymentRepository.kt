package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.BillPayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BillPaymentRepository : JpaRepository<BillPayment, String> {
    fun findByBillIdAndOrganizationId(
        billId: String,
        organizationId: String,
    ): List<BillPayment>

    fun findByOrganizationId(organizationId: String): List<BillPayment>
}
