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
class Organizations(
    @Id
    @Column(name = "uuid", columnDefinition = "uuid")
    var uuid: String = UUID.randomUUID().toString(),
    @Column(name = "org_slug")
    var orgSlug: String,
    var name: String,
    var description: String? = null,
    @Column(name = "legal_name")
    var legalName: String,
    @Column(name = "trade_name")
    var tradeName: String,
    @Column(name = "base_currency", columnDefinition = "char(3)")
    var baseCurrency: String = "USD",
    @Column(name = "fiscal_year_start")
    var fiscalYearStart: LocalDateTime,
    var timezone: String,
    var status: String = "ACTIVE",
    @Column(name = "inventory_costing_method")
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    var inventoryCostingMethod: InventoryCostingMethod = InventoryCostingMethod.WEIGHTED_AVERAGE,
    @Column(name = "inventory_gl_posting_enabled")
    var inventoryGlPostingEnabled: Boolean = false,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @Column(name = "is_active")
    var isActive: Boolean = true,
)

enum class InventoryCostingMethod {
    FIFO,
    WEIGHTED_AVERAGE,
}
