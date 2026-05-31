package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import com.github.f4b6a3.uuid.UuidCreator

@Entity
@Table(name = "crm_pipeline_stages")
@EntityListeners(AuditingEntityListener::class)
data class PipelineStage(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UuidCreator.getTimeOrderedEpoch(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: UUID,
    val code: String,
    val name: String,
    val description: String? = null,
    @Column(name = "sort_order")
    val sortOrder: Int = 0,
    @Column(name = "probability_pct")
    val probabilityPct: BigDecimal = BigDecimal.ZERO,
    @Column(name = "is_won")
    val isWon: Boolean = false,
    @Column(name = "is_lost")
    val isLost: Boolean = false,
    @Column(name = "is_active")
    val isActive: Boolean = true,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
