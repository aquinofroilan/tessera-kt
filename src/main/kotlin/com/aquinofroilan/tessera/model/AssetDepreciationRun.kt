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

enum class DepreciationRunStatus {
    DRAFT,
    POSTED,
    CANCELLED,
}

@Entity
@Table(name = "asset_depreciation_runs")
@EntityListeners(AuditingEntityListener::class)
data class AssetDepreciationRun(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "period_year")
    val periodYear: Int,
    @Column(name = "period_month")
    val periodMonth: Int,
    @Enumerated(EnumType.STRING)
    val status: DepreciationRunStatus = DepreciationRunStatus.DRAFT,
    @Column(name = "total_depreciation")
    val totalDepreciation: BigDecimal = BigDecimal.ZERO,
    @Column(name = "journal_entry_id", columnDefinition = "uuid")
    val journalEntryId: String? = null,
    @Column(name = "posted_at")
    val postedAt: LocalDateTime? = null,
    @Column(name = "posted_by", columnDefinition = "uuid")
    val postedBy: String? = null,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
