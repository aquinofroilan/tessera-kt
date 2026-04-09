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

enum class BillStatus {
    DRAFT,
    APPROVED,
    PARTIALLY_PAID,
    PAID,
    VOID,
}

enum class PaymentMethod {
    CASH,
    CHECK,
    BANK_TRANSFER,
    CREDIT_CARD,
    OTHER,
}

data class BillLine(
    val accountId: String,
    val accountCode: String,
    val accountName: String,
    val amount: BigDecimal,
    val description: String? = null,
)

@Document(collection = "bills")
@CompoundIndex(
    name = "unique_bill_number_per_org",
    def = "{'organizationId': 1, 'billNumber': 1}",
    unique = true,
)
data class Bill(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val billNumber: String,
    val vendorId: String,
    val vendorName: String,
    val date: LocalDate,
    val dueDate: LocalDate,
    val referenceNumber: String? = null,
    @Indexed
    val organizationId: String,
    val status: BillStatus = BillStatus.DRAFT,
    val lines: List<BillLine>,
    val totalAmount: BigDecimal,
    val amountPaid: BigDecimal = BigDecimal.ZERO,
    val journalEntryId: String? = null,
    val createdBy: String,
    val approvedAt: LocalDateTime? = null,
    val approvedBy: String? = null,
    val paidAt: LocalDateTime? = null,
    val voidedAt: LocalDateTime? = null,
    val voidReason: String? = null,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
