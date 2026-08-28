package com.aquinofroilan.tessera.domain.assets.model

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

enum class DisposalType {
    SALE,
    WRITE_OFF,
    SCRAP,
}

enum class DisposalStatus {
    DRAFT,
    POSTED,
    CANCELLED,
}

@Entity
@Table(name = "asset_disposals")
@EntityListeners(AuditingEntityListener::class)
data class AssetDisposal(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: UUID,
    @Column(name = "asset_id", columnDefinition = "uuid")
    val assetId: UUID,
    @Column(name = "disposal_date")
    val disposalDate: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(name = "disposal_type")
    val disposalType: DisposalType,
    val proceeds: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    val status: DisposalStatus = DisposalStatus.DRAFT,
    @Column(name = "journal_entry_id", columnDefinition = "uuid")
    val journalEntryId: UUID? = null,
    @Column(name = "gain_loss_account_id", columnDefinition = "uuid")
    val gainLossAccountId: UUID? = null,
    @Column(name = "cash_account_id", columnDefinition = "uuid")
    val cashAccountId: UUID? = null,
    val notes: String? = null,
    @Column(name = "posted_at")
    val postedAt: LocalDateTime? = null,
    @Column(name = "posted_by", columnDefinition = "uuid")
    val postedBy: UUID? = null,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
