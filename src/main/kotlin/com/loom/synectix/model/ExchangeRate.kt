package com.loom.synectix.model

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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class ExchangeRateSource {
    MANUAL,
    AUTO,
}

@Entity
@Table(name = "exchange_rates")
@EntityListeners(AuditingEntityListener::class)
data class ExchangeRate(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "from_currency", columnDefinition = "char(3)")
    val fromCurrency: String,
    @Column(name = "to_currency", columnDefinition = "char(3)")
    val toCurrency: String,
    val rate: BigDecimal,
    @Column(name = "as_of_date")
    val asOfDate: LocalDate,
    @Enumerated(EnumType.STRING)
    val source: ExchangeRateSource = ExchangeRateSource.MANUAL,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
