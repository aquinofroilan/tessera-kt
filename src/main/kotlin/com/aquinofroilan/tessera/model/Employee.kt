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
data class Employee(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "employee_number")
    val employeeNumber: String,
    @Column(name = "first_name")
    val firstName: String,
    @Column(name = "last_name")
    val lastName: String,
    val email: String? = null,
    @Column(name = "job_title")
    val jobTitle: String? = null,
    @Column(name = "department_id", columnDefinition = "uuid")
    val departmentId: String? = null,
    @Column(name = "hire_date")
    val hireDate: LocalDate,
    @Enumerated(EnumType.STRING)
    val status: EmploymentStatus = EmploymentStatus.ACTIVE,
    @Column(name = "termination_date")
    val terminationDate: LocalDate? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
