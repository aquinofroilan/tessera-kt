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

enum class AssetStatus {
    ACTIVE,
    DISPOSED,
    FULLY_DEPRECIATED,
}

@Entity
@Table(name = "fixed_assets")
@EntityListeners(AuditingEntityListener::class)
class FixedAsset(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    @Column(name = "asset_number")
    var assetNumber: String,
    var name: String,
    var description: String? = null,
    @Column(name = "category_id", columnDefinition = "uuid")
    var categoryId: String? = null,
    @Column(name = "acquisition_date")
    var acquisitionDate: LocalDate,
    @Column(name = "acquisition_cost")
    var acquisitionCost: BigDecimal,
    @Column(name = "salvage_value")
    var salvageValue: BigDecimal = BigDecimal.ZERO,
    @Column(name = "useful_life_months")
    var usefulLifeMonths: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "depreciation_method")
    var depreciationMethod: DepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
    var location: String? = null,
    @Column(name = "serial_number")
    var serialNumber: String? = null,
    @Enumerated(EnumType.STRING)
    var status: AssetStatus = AssetStatus.ACTIVE,
    @Column(name = "accumulated_depreciation")
    var accumulatedDepreciation: BigDecimal = BigDecimal.ZERO,
    @Column(name = "asset_account_id", columnDefinition = "uuid")
    var assetAccountId: String? = null,
    @Column(name = "accumulated_depreciation_account_id", columnDefinition = "uuid")
    var accumulatedDepreciationAccountId: String? = null,
    @Column(name = "depreciation_expense_account_id", columnDefinition = "uuid")
    var depreciationExpenseAccountId: String? = null,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
