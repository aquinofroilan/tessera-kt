package com.froilan.synectix.repository

import com.froilan.synectix.model.BillPayment
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface BillPaymentRepository : MongoRepository<BillPayment, String> {
    fun findByBillId(billId: String): List<BillPayment>

    fun findByOrganizationId(organizationId: String): List<BillPayment>
}
