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
import java.time.LocalDateTime
import java.util.UUID

enum class DepreciationMethod {
    STRAIGHT_LINE,
}

@Entity
@Table(name = "asset_categories")
@EntityListeners(AuditingEntityListener::class)
class AssetCategory(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    var code: String,
    var name: String,
    var description: String? = null,
    @Column(name = "default_useful_life_months")
    var defaultUsefulLifeMonths: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "default_depreciation_method")
    var defaultDepreciationMethod: DepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
    @Column(name = "default_salvage_value")
    var defaultSalvageValue: BigDecimal = BigDecimal.ZERO,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
