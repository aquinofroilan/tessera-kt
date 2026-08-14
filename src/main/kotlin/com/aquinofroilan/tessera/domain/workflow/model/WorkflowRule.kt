package com.aquinofroilan.tessera.domain.workflow.model

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
import java.time.LocalDateTime
import java.util.UUID

enum class WorkflowRuleActionType {
    NOTIFY_USER,
    NOTIFY_ROLE,
}

@Entity
@Table(name = "workflow_rules")
@EntityListeners(AuditingEntityListener::class)
data class WorkflowRule(
    @Id
    @Column(columnDefinition = "uuid")
    val id: java.util.UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: java.util.UUID,
    val name: String,
    val description: String? = null,
    @Column(name = "event_kind")
    val eventKind: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    val actionType: WorkflowRuleActionType,
    @Column(name = "action_target")
    val actionTarget: String,
    val enabled: Boolean = true,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
