package com.aquinofroilan.tessera.domain.project.model

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
import java.time.LocalDateTime
import java.util.UUID

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED,
}

@Entity
@Table(name = "project_tasks")
@EntityListeners(AuditingEntityListener::class)
class ProjectTask(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "project_id", columnDefinition = "uuid")
    var projectId: java.util.UUID,
    @Column(name = "parent_task_id", columnDefinition = "uuid")
    var parentTaskId: java.util.UUID? = null,
    var name: String,
    var description: String? = null,
    @Column(name = "assignee_employee_id", columnDefinition = "uuid")
    var assigneeEmployeeId: java.util.UUID? = null,
    @Column(name = "estimated_hours")
    var estimatedHours: BigDecimal? = null,
    @Column(name = "planned_start_date")
    var plannedStartDate: java.time.LocalDate? = null,
    @Column(name = "planned_finish_date")
    var plannedFinishDate: java.time.LocalDate? = null,
    @Column(name = "actual_start_date")
    var actualStartDate: java.time.LocalDate? = null,
    @Column(name = "actual_finish_date")
    var actualFinishDate: java.time.LocalDate? = null,
    @Enumerated(EnumType.STRING)
    var status: TaskStatus = TaskStatus.TODO,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
