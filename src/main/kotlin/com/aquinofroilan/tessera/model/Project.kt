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
class Project(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "project_number")
    var projectNumber: String,
    var name: String,
    var description: String? = null,
    @Column(name = "customer_id", columnDefinition = "uuid")
    var customerId: java.util.UUID? = null,
    @Column(name = "manager_employee_id", columnDefinition = "uuid")
    var managerEmployeeId: java.util.UUID? = null,
    @Column(name = "start_date")
    var startDate: LocalDate,
    @Column(name = "end_date")
    var endDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    var status: ProjectStatus = ProjectStatus.PLANNED,
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type")
    var billingType: ProjectBillingType = ProjectBillingType.TIME_AND_MATERIALS,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
