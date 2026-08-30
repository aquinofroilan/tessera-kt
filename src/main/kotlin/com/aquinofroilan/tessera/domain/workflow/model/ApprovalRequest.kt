package com.aquinofroilan.tessera.domain.workflow.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.ZonedDateTime
import java.util.UUID

enum class ApprovalRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
}

@Entity
@Table(name = "approval_requests")
data class ApprovalRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", nullable = false)
    val organizationId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    val workflow: ApprovalWorkflow,
    @Column(name = "entity_type", nullable = false)
    val entityType: String,
    @Column(name = "entity_id", nullable = false)
    val entityId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ApprovalRequestStatus = ApprovalRequestStatus.PENDING,
    @Column(name = "current_step_order", nullable = false)
    var currentStepOrder: Int = 1,
    @Column(name = "requester_id", nullable = false)
    val requesterId: UUID,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime = ZonedDateTime.now(),
)
