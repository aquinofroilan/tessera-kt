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

enum class EmploymentStatus {
    ACTIVE,
    ON_LEAVE,
    TERMINATED,
}

@Entity
@Table(name = "employees")
@EntityListeners(AuditingEntityListener::class)
class Employee(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    @Column(name = "employee_number")
    var employeeNumber: String,
    @Column(name = "first_name")
    var firstName: String,
    @Column(name = "last_name")
    var lastName: String,
    var email: String? = null,
    @Column(name = "job_title")
    var jobTitle: String? = null,
    @Column(name = "department_id", columnDefinition = "uuid")
    var departmentId: String? = null,
    @Column(name = "user_id", columnDefinition = "uuid")
    var userId: String? = null,
    @Column(name = "hire_date")
    var hireDate: LocalDate,
    @Enumerated(EnumType.STRING)
    var status: EmploymentStatus = EmploymentStatus.ACTIVE,
    @Column(name = "termination_date")
    var terminationDate: LocalDate? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
