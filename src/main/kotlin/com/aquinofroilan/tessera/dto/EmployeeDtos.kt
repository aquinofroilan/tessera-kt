package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.Employee
import com.aquinofroilan.tessera.model.EmploymentStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateEmployeeRequest(
    @field:NotBlank(message = "First name is required")
    val firstName: String,
    @field:NotBlank(message = "Last name is required")
    val lastName: String,
    @field:Email(message = "Email must be valid")
    val email: String? = null,
    val jobTitle: String? = null,
    val departmentId: String? = null,
    val userId: String? = null,
    @field:NotNull(message = "Hire date is required")
    val hireDate: LocalDate?,
)

data class UpdateEmployeeRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    @field:Email(message = "Email must be valid")
    val email: String? = null,
    val jobTitle: String? = null,
    val userId: String? = null,
)

data class TerminateEmployeeRequest(
    @field:NotNull(message = "Termination date is required")
    val terminationDate: LocalDate?,
)

data class EmployeeResponse(
    val id: String,
    val employeeNumber: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val jobTitle: String?,
    val departmentId: String?,
    val userId: String?,
    val hireDate: String,
    val status: EmploymentStatus,
    val terminationDate: String?,
    val organizationId: String,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(employee: Employee) =
            EmployeeResponse(
                id = employee.id,
                employeeNumber = employee.employeeNumber,
                firstName = employee.firstName,
                lastName = employee.lastName,
                email = employee.email,
                jobTitle = employee.jobTitle,
                departmentId = employee.departmentId,
                userId = employee.userId,
                hireDate = employee.hireDate.toString(),
                status = employee.status,
                terminationDate = employee.terminationDate?.toString(),
                organizationId = employee.organizationId,
                createdAt = employee.createdAt?.toString(),
                updatedAt = employee.updatedAt?.toString(),
            )
    }
}
