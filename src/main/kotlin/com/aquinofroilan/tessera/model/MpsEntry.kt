package com.aquinofroilan.tessera.model

import com.github.f4b6a3.uuid.UuidCreator
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

enum class MpsStatus {
    PLANNED,
    FIRM,
    RELEASED,
    CANCELLED,
}

@Entity
@Table(name = "mfg_mps_entries")
@EntityListeners(AuditingEntityListener::class)
data class MpsEntry(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UuidCreator.getTimeOrderedEpoch(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: UUID,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: java.util.UUID,
    @Column(name = "product_sku")
    val productSku: String,
    @Column(name = "product_name")
    val productName: String,
    val quantity: BigDecimal,
    @Column(name = "required_by")
    val requiredBy: LocalDate,
    @Enumerated(EnumType.STRING)
    val status: MpsStatus = MpsStatus.PLANNED,
    val notes: String? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
