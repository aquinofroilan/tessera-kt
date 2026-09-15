package com.aquinofroilan.tessera.domain.hr.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

enum class ChecklistType {
    ONBOARDING,
    OFFBOARDING,
}

@Entity
@Table(name = "checklist_templates")
@EntityListeners(AuditingEntityListener::class)
class ChecklistTemplate(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    var name: String,
    @Enumerated(EnumType.STRING)
    var type: ChecklistType,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "template_id")
    var tasks: MutableList<ChecklistTaskTemplate> = mutableListOf(),
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)

@Entity
@Table(name = "checklist_task_templates")
class ChecklistTaskTemplate(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "template_id", columnDefinition = "uuid", insertable = false, updatable = false)
    var templateId: java.util.UUID? = null,
    var description: String,
    @Column(name = "is_required")
    var isRequired: Boolean = true,
    @Column(name = "sort_order")
    var sortOrder: Int = 0,
)

@Entity
@Table(name = "employee_checklists")
@EntityListeners(AuditingEntityListener::class)
class EmployeeChecklist(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID,
    @Enumerated(EnumType.STRING)
    var type: ChecklistType,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "checklist_id")
    var tasks: MutableList<EmployeeChecklistTask> = mutableListOf(),
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
) {
    fun isComplete(): Boolean = tasks.filter { it.isRequired }.all { it.isCompleted }
}

@Entity
@Table(name = "employee_checklist_tasks")
class EmployeeChecklistTask(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "checklist_id", columnDefinition = "uuid", insertable = false, updatable = false)
    var checklistId: java.util.UUID? = null,
    var description: String,
    @Column(name = "is_required")
    var isRequired: Boolean = true,
    @Column(name = "is_completed")
    var isCompleted: Boolean = false,
    @Column(name = "sort_order")
    var sortOrder: Int = 0,
    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,
    @Column(name = "completed_by", columnDefinition = "uuid")
    var completedBy: java.util.UUID? = null,
)
