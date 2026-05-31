package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "organizations")
@EntityListeners(AuditingEntityListener::class)
data class Organizations(
    @Id
    @Column(name = "uuid", columnDefinition = "uuid")
    val uuid: String = UUID.randomUUID().toString(),
    @Column(name = "org_slug")
    val orgSlug: String,
    val name: String,
    val description: String? = null,
    @Column(name = "legal_name")
    val legalName: String,
    @Column(name = "trade_name")
    val tradeName: String,
    @Column(name = "base_currency", columnDefinition = "char(3)")
    val baseCurrency: String = "USD",
    @Column(name = "fiscal_year_start")
    val fiscalYearStart: LocalDateTime,
    val timezone: String,
    val status: String = "ACTIVE",
    @Column(name = "inventory_costing_method")
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    val inventoryCostingMethod: InventoryCostingMethod = InventoryCostingMethod.WEIGHTED_AVERAGE,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @Column(name = "is_active")
    val isActive: Boolean = true,
)

enum class InventoryCostingMethod {
    FIFO,
    WEIGHTED_AVERAGE,
}
