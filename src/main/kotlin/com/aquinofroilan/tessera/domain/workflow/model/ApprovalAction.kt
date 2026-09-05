package com.aquinofroilan.tessera.domain.workflow.model

import com.aquinofroilan.tessera.domain.auth.model.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.ZonedDateTime
import java.util.UUID

enum class ApprovalActionType {
    APPROVED,
    REJECTED,
}

@Entity
@Table(name = "approval_actions")
data class ApprovalAction(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: ApprovalRequest,
    @Column(name = "step_order", nullable = false)
    val stepOrder: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    val actor: User,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val action: ApprovalActionType,
    @Column
    val comments: String? = null,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
)
