package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.InvoiceReceipt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InvoiceReceiptRepository : JpaRepository<InvoiceReceipt, String> {
    fun findByInvoiceIdAndOrganizationId(
        invoiceId: String,
        organizationId: String,
    ): List<InvoiceReceipt>

    fun findByOrganizationId(organizationId: String): List<InvoiceReceipt>
}
