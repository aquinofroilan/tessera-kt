package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "tax_rates")
@CompoundIndex(
    name = "unique_tax_rate_code_per_org",
    def = "{'organizationId': 1, 'code': 1}",
    unique = true,
)
data class TaxRate(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val code: String,
    val percentage: BigDecimal,
    val authority: String,
    @Indexed
    val organizationId: String,
    val isActive: Boolean = true,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
