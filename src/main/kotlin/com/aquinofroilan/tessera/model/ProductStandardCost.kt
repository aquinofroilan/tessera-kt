package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "mfg_product_standard_costs")
data class ProductStandardCost(
    @Id
    @Column(columnDefinition = "uuid")
    val id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: java.util.UUID,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: java.util.UUID,
    @Column(name = "bom_id", columnDefinition = "uuid")
    val bomId: java.util.UUID? = null,
    @Column(name = "routing_id", columnDefinition = "uuid")
    val routingId: java.util.UUID? = null,
    @Column(name = "material_cost")
    val materialCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "labor_cost")
    val laborCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "overhead_cost")
    val overheadCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "total_cost")
    val totalCost: BigDecimal = BigDecimal.ZERO,
    val source: String = "rollup",
    @Column(name = "calculated_at")
    val calculatedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "calculated_by", columnDefinition = "uuid")
    val calculatedBy: String,
    val notes: String? = null,
)
