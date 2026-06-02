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
data class FixedAsset(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "asset_number")
    val assetNumber: String,
    val name: String,
    val description: String? = null,
    @Column(name = "category_id", columnDefinition = "uuid")
    val categoryId: String? = null,
    @Column(name = "acquisition_date")
    val acquisitionDate: LocalDate,
    @Column(name = "acquisition_cost")
    val acquisitionCost: BigDecimal,
    @Column(name = "salvage_value")
    val salvageValue: BigDecimal = BigDecimal.ZERO,
    @Column(name = "useful_life_months")
    val usefulLifeMonths: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "depreciation_method")
    val depreciationMethod: DepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
    val location: String? = null,
    @Column(name = "serial_number")
    val serialNumber: String? = null,
    @Enumerated(EnumType.STRING)
    val status: AssetStatus = AssetStatus.ACTIVE,
    @Column(name = "accumulated_depreciation")
    val accumulatedDepreciation: BigDecimal = BigDecimal.ZERO,
    @Column(name = "asset_account_id", columnDefinition = "uuid")
    val assetAccountId: String? = null,
    @Column(name = "accumulated_depreciation_account_id", columnDefinition = "uuid")
    val accumulatedDepreciationAccountId: String? = null,
    @Column(name = "depreciation_expense_account_id", columnDefinition = "uuid")
    val depreciationExpenseAccountId: String? = null,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
