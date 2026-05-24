package com.froilan.synectix.repository

import com.froilan.synectix.model.BillPayment
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
