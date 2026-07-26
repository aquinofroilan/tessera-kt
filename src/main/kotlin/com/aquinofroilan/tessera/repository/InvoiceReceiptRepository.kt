package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.InvoiceReceipt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InvoiceReceiptRepository : JpaRepository<InvoiceReceipt, java.util.UUID> {
    fun findByInvoiceIdAndOrganizationId(
        invoiceId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<InvoiceReceipt>

    fun findByOrganizationId(organizationId: java.util.UUID): List<InvoiceReceipt>
}
