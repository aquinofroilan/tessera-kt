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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class ProjectStatus {
    PLANNED,
    ACTIVE,
    ON_HOLD,
    CLOSED,
    CANCELLED,
}

enum class ProjectBillingType {
    TIME_AND_MATERIALS,
    FIXED_PRICE,
    MILESTONE,
}

@Entity
@Table(name = "projects")
@EntityListeners(AuditingEntityListener::class)
data class Project(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "project_number")
    val projectNumber: String,
    val name: String,
    val description: String? = null,
    @Column(name = "customer_id", columnDefinition = "uuid")
    val customerId: String? = null,
    @Column(name = "manager_employee_id", columnDefinition = "uuid")
    val managerEmployeeId: String? = null,
    @Column(name = "start_date")
    val startDate: LocalDate,
    @Column(name = "end_date")
    val endDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    val status: ProjectStatus = ProjectStatus.PLANNED,
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type")
    val billingType: ProjectBillingType = ProjectBillingType.TIME_AND_MATERIALS,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
