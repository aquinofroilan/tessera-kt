package com.aquinofroilan.tessera.domain.workflow.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "approval_workflow_steps")
data class ApprovalWorkflowStep(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    var workflow: ApprovalWorkflow? = null,
    @Column(name = "step_order", nullable = false)
    var stepOrder: Int,
    @Column(nullable = false)
    var name: String,
    @Column(name = "required_approvals", nullable = false)
    var requiredApprovals: Int = 1,
    @OneToMany(mappedBy = "step", cascade = [CascadeType.ALL], orphanRemoval = true)
    var approvers: MutableList<WorkflowStepApprover> = mutableListOf(),
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime = ZonedDateTime.now(),
) {
    fun addApprover(approver: WorkflowStepApprover) {
        approvers.add(approver)
        approver.step = this
    }

    fun removeApprover(approver: WorkflowStepApprover) {
        approvers.remove(approver)
    }
}
