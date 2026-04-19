package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class InvoiceStatus {
    DRAFT,
    APPROVED,
    PARTIALLY_PAID,
    PAID,
    VOID,
}

data class InvoiceLine(
    val accountId: String,
    val accountCode: String,
    val accountName: String,
    val amount: BigDecimal,
    val description: String? = null,
)

@Document(collection = "invoices")
@CompoundIndex(
    name = "unique_invoice_number_per_org",
    def = "{'organizationId': 1, 'invoiceNumber': 1}",
    unique = true,
)
@CompoundIndex(name = "org_status", def = "{'organizationId': 1, 'status': 1}")
@CompoundIndex(name = "org_customer", def = "{'organizationId': 1, 'customerId': 1}")
data class Invoice(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val customerId: String,
    val customerName: String,
    val date: LocalDate,
    val dueDate: LocalDate,
    val referenceNumber: String? = null,
    val taxGroupId: String? = null,
    @Indexed
    val organizationId: String,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val lines: List<InvoiceLine>,
    val totalAmount: BigDecimal,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val amountReceived: BigDecimal = BigDecimal.ZERO,
    val journalEntryId: String? = null,
    val createdBy: String,
    val approvedAt: LocalDateTime? = null,
    val approvedBy: String? = null,
    val paidAt: LocalDateTime? = null,
    val voidedAt: LocalDateTime? = null,
    val voidedBy: String? = null,
    val voidReason: String? = null,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
