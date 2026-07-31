package com.aquinofroilan.tessera.model

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
class ExchangeRate(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "from_currency", columnDefinition = "char(3)")
    var fromCurrency: String,
    @Column(name = "to_currency", columnDefinition = "char(3)")
    var toCurrency: String,
    var rate: BigDecimal,
    @Column(name = "as_of_date")
    var asOfDate: LocalDate,
    @Enumerated(EnumType.STRING)
    var source: ExchangeRateSource = ExchangeRateSource.MANUAL,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
