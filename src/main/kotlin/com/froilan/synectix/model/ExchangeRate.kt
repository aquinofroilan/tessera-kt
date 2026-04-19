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

enum class ExchangeRateSource {
    MANUAL,
    AUTO,
}

@Document(collection = "exchange_rates")
@CompoundIndex(
    name = "unique_pair_per_org_per_date",
    def = "{'organizationId': 1, 'fromCurrency': 1, 'toCurrency': 1, 'asOfDate': 1}",
    unique = true,
)
data class ExchangeRate(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Indexed
    val organizationId: String,
    val fromCurrency: String,
    val toCurrency: String,
    val rate: BigDecimal,
    val asOfDate: LocalDate,
    val source: ExchangeRateSource = ExchangeRateSource.MANUAL,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
