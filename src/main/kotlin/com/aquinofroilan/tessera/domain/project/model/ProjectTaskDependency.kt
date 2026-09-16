package com.aquinofroilan.tessera.domain.project.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

enum class TaskDependencyType {
    FINISH_TO_START,
    START_TO_START,
    FINISH_TO_FINISH,
    START_TO_FINISH,
}

@Entity
@Table(name = "project_task_dependencies")
@EntityListeners(AuditingEntityListener::class)
class ProjectTaskDependency(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "predecessor_task_id", columnDefinition = "uuid")
    var predecessorTaskId: java.util.UUID,
    @Column(name = "successor_task_id", columnDefinition = "uuid")
    var successorTaskId: java.util.UUID,
    @Enumerated(EnumType.STRING)
    var dependencyType: TaskDependencyType,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
