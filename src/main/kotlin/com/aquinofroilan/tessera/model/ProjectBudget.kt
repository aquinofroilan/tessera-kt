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

enum class ProjectCostCategory {
    LABOR,
    MATERIAL,
    EXPENSE,
    OTHER,
}

@Entity
@Table(name = "project_budgets")
@EntityListeners(AuditingEntityListener::class)
class ProjectBudget(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = UUID.randomUUID(),
    @Column(name = "project_id", columnDefinition = "uuid")
    var projectId: java.util.UUID,
    @Enumerated(EnumType.STRING)
    var category: ProjectCostCategory,
    @Column(name = "budget_amount")
    var budgetAmount: BigDecimal,
    var currency: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
