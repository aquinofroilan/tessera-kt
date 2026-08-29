package com.aquinofroilan.tessera.domain.organization.model

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
    var uuid: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
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
    @Column(name = "logo_url")
    var logoUrl: String? = null,
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    var status: OrganizationStatus = OrganizationStatus.ACTIVE,
    @Column(name = "billing_plan")
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    var billingPlan: BillingPlan = BillingPlan.FREE,
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

enum class OrganizationStatus {
    ACTIVE,
    SUSPENDED,
    ARCHIVED,
}

enum class BillingPlan {
    FREE,
    STARTER,
    ENTERPRISE,
}

enum class FeatureFlag {
    API_KEYS,
    MULTI_CURRENCY,
    ADVANCED_ANALYTICS,
    CUSTOM_ROLES,
    MFG_MODULE,
    ASSET_MANAGEMENT,
    WORKFLOW_AUTOMATION,
    AUDIT_EXPORT,
    CUSTOM,
}

enum class InventoryCostingMethod {
    FIFO,
    WEIGHTED_AVERAGE,
}
