package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "bill_payments")
data class BillPayment(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Indexed
    val billId: String,
    val paymentDate: LocalDate,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val referenceNumber: String? = null,
    val journalEntryId: String? = null,
    @Indexed
    val organizationId: String,
    val createdBy: String,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
)
