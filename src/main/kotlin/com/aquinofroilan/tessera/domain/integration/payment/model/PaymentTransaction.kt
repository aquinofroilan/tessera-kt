package com.aquinofroilan.tessera.domain.integration.payment.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class GatewayType {
    STRIPE,
    PAYMONGO,
}

enum class PaymentTransactionStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
}

@Entity
@Table(name = "payment_transactions")
@EntityListeners(AuditingEntityListener::class)
class PaymentTransaction(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "invoice_id", columnDefinition = "uuid")
    var invoiceId: UUID,
    @Enumerated(EnumType.STRING)
    var gateway: GatewayType,
    @Column(name = "gateway_intent_id")
    var gatewayIntentId: String,
    @Column(name = "gross_amount")
    var grossAmount: BigDecimal,
    @Column(name = "fee_amount")
    var feeAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "net_amount")
    var netAmount: BigDecimal = grossAmount,
    var currency: String,
    @Enumerated(EnumType.STRING)
    var status: PaymentTransactionStatus = PaymentTransactionStatus.PENDING,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
