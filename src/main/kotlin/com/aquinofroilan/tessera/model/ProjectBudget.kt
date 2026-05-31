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
data class ProjectBudget(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "project_id", columnDefinition = "uuid")
    val projectId: String,
    @Enumerated(EnumType.STRING)
    val category: ProjectCostCategory,
    @Column(name = "budget_amount")
    val budgetAmount: BigDecimal,
    val currency: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
