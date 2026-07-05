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
    var id: String = UUID.randomUUID().toString(),
    @Column(name = "project_id", columnDefinition = "uuid")
    var projectId: String,
    @Column(name = "parent_task_id", columnDefinition = "uuid")
    var parentTaskId: String? = null,
    var name: String,
    var description: String? = null,
    @Column(name = "assignee_employee_id", columnDefinition = "uuid")
    var assigneeEmployeeId: String? = null,
    @Column(name = "estimated_hours")
    var estimatedHours: BigDecimal? = null,
    @Enumerated(EnumType.STRING)
    var status: TaskStatus = TaskStatus.TODO,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
