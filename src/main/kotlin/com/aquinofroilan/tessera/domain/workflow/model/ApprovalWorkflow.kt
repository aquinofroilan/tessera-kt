package com.aquinofroilan.tessera.domain.workflow.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "approval_workflows")
data class ApprovalWorkflow(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", nullable = false)
    val organizationId: UUID,
    @Column(nullable = false)
    var name: String,
    @Column
    var description: String? = null,
    @Column(name = "entity_type", nullable = false)
    var entityType: String,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @OneToMany(mappedBy = "workflow", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    var steps: MutableList<ApprovalWorkflowStep> = mutableListOf(),
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime = ZonedDateTime.now(),
    @Column(name = "created_by")
    val createdBy: UUID? = null,
    @Column(name = "updated_by")
    var updatedBy: UUID? = null,
) {
    fun addStep(step: ApprovalWorkflowStep) {
        steps.add(step)
        step.workflow = this
    }

    fun removeStep(step: ApprovalWorkflowStep) {
        steps.remove(step)
    }
}
