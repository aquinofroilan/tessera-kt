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

enum class InventoryCountStatus {
    DRAFT,
    COUNTING,
    POSTED,
    CANCELLED,
}

@Entity
@Table(name = "inventory_count_lines")
data class InventoryCountLine(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "line_number")
    val lineNumber: Int,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: String,
    @Column(name = "product_sku")
    val productSku: String,
    @Column(name = "product_name")
    val productName: String,
    @Column(name = "expected_quantity")
    val expectedQuantity: BigDecimal,
    @Column(name = "counted_quantity")
    val countedQuantity: BigDecimal? = null,
    @Column(name = "variance_quantity")
    val varianceQuantity: BigDecimal? = null,
    val notes: String? = null,
    @Column(name = "adjustment_movement_id", columnDefinition = "uuid")
    val adjustmentMovementId: String? = null,
)

@Entity
@Table(name = "inventory_count_sessions")
@EntityListeners(AuditingEntityListener::class)
data class InventoryCountSession(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    val code: String,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    val warehouseId: String,
    @Enumerated(EnumType.STRING)
    val status: InventoryCountStatus = InventoryCountStatus.DRAFT,
    @Column(name = "scheduled_for")
    val scheduledFor: LocalDate? = null,
    @Column(name = "started_at")
    val startedAt: LocalDateTime? = null,
    @Column(name = "posted_at")
    val postedAt: LocalDateTime? = null,
    @Column(name = "posted_by", columnDefinition = "uuid")
    val postedBy: String? = null,
    val notes: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id")
    @OrderBy("lineNumber ASC")
    val lines: List<InventoryCountLine>,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
