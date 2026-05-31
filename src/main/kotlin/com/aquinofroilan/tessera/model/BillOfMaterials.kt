package com.aquinofroilan.tessera.model

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
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class BomStatus {
    DRAFT,
    ACTIVE,
    OBSOLETE,
}

@Entity
@Table(name = "mfg_bom_lines")
data class BomLine(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "line_number")
    val lineNumber: Int,
    @Column(name = "component_product_id", columnDefinition = "uuid")
    val componentProductId: String,
    @Column(name = "component_sku")
    val componentSku: String,
    @Column(name = "component_name")
    val componentName: String,
    val quantity: BigDecimal,
    val uom: String? = null,
    @Column(name = "scrap_pct")
    val scrapPct: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null,
)

@Entity
@Table(name = "mfg_boms")
@EntityListeners(AuditingEntityListener::class)
data class BillOfMaterials(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: String,
    val code: String,
    val name: String,
    val version: Int = 1,
    @Enumerated(EnumType.STRING)
    val status: BomStatus = BomStatus.DRAFT,
    @Column(name = "is_default")
    val isDefault: Boolean = false,
    @Column(name = "effective_from")
    val effectiveFrom: LocalDate? = null,
    @Column(name = "effective_to")
    val effectiveTo: LocalDate? = null,
    val notes: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "bom_id")
    @OrderBy("lineNumber ASC")
    val lines: List<BomLine>,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @Column(name = "activated_at")
    val activatedAt: LocalDateTime? = null,
    @Column(name = "activated_by", columnDefinition = "uuid")
    val activatedBy: String? = null,
    @Column(name = "obsoleted_at")
    val obsoletedAt: LocalDateTime? = null,
    @Column(name = "obsoleted_by", columnDefinition = "uuid")
    val obsoletedBy: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
