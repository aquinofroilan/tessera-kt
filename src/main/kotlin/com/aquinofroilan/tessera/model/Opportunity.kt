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

enum class OpportunityStatus {
    OPEN,
    WON,
    LOST,
    ABANDONED,
}

@Entity
@Table(name = "crm_opportunities")
@EntityListeners(AuditingEntityListener::class)
data class Opportunity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    val name: String,
    @Column(name = "customer_id", columnDefinition = "uuid")
    val customerId: String,
    @Column(name = "primary_contact_id", columnDefinition = "uuid")
    val primaryContactId: String? = null,
    @Column(name = "stage_id", columnDefinition = "uuid")
    val stageId: String,
    val amount: BigDecimal = BigDecimal.ZERO,
    @Column(columnDefinition = "char(3)")
    val currency: String,
    @Column(name = "expected_close_date")
    val expectedCloseDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    val status: OpportunityStatus = OpportunityStatus.OPEN,
    @Column(name = "owner_user_id", columnDefinition = "uuid")
    val ownerUserId: String? = null,
    @Column(name = "source_lead_id", columnDefinition = "uuid")
    val sourceLeadId: String? = null,
    val notes: String? = null,
    @Column(name = "closed_at")
    val closedAt: LocalDateTime? = null,
    @Column(name = "closed_by", columnDefinition = "uuid")
    val closedBy: String? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
