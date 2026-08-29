package com.aquinofroilan.tessera.domain.mfg.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class EcoStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    IMPLEMENTED
}

enum class EcoItemType {
    BOM,
    ROUTING
}

@Entity
@Table(name = "mfg_eco_affected_items")
data class EcoAffectedItem(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    val itemType: EcoItemType,

    @Column(name = "old_version_id", columnDefinition = "uuid")
    val oldVersionId: UUID? = null,

    @Column(name = "new_version_id", columnDefinition = "uuid")
    val newVersionId: UUID,

    val notes: String? = null
)

@Entity
@Table(name = "mfg_engineering_change_orders")
@EntityListeners(AuditingEntityListener::class)
data class EngineeringChangeOrder(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: UUID,

    @Column(name = "eco_number")
    val ecoNumber: String,

    val title: String,
    
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    var status: EcoStatus = EcoStatus.DRAFT,

    @Column(name = "effective_date")
    val effectiveDate: LocalDate? = null,

    @Column(name = "requested_by", columnDefinition = "uuid")
    val requestedBy: UUID,

    @Column(name = "approved_by", columnDefinition = "uuid")
    var approvedBy: UUID? = null,

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    @Column(name = "implemented_at")
    var implementedAt: LocalDateTime? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "eco_id")
    val affectedItems: MutableList<EcoAffectedItem> = mutableListOf(),

    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
) {
    fun addAffectedItem(item: EcoAffectedItem) {
        affectedItems.add(item)
    }
}
