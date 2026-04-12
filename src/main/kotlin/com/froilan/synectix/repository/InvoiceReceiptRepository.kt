package com.froilan.synectix.repository

import com.froilan.synectix.model.InvoiceReceipt
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface InvoiceReceiptRepository : MongoRepository<InvoiceReceipt, String> {
    fun findByInvoiceIdAndOrganizationId(
        invoiceId: String,
        organizationId: String,
    ): List<InvoiceReceipt>

    fun findByOrganizationId(organizationId: String): List<InvoiceReceipt>
}
