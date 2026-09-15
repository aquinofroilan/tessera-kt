package com.aquinofroilan.tessera.domain.hr.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateTrainingCourseRequest(
    @field:NotEmpty(message = "Name is required")
    val name: String?,
    val description: String? = null,
    val provider: String? = null,
)

data class RecordTrainingRequest(
    @field:NotNull(message = "Employee ID is required")
    val employeeId: java.util.UUID?,
    @field:NotNull(message = "Course ID is required")
    val courseId: java.util.UUID?,
    @field:NotNull(message = "Completion date is required")
    val completionDate: LocalDate?,
    val attachmentId: java.util.UUID? = null,
)

data class RecordCertificationRequest(
    @field:NotNull(message = "Employee ID is required")
    val employeeId: java.util.UUID?,
    @field:NotEmpty(message = "Name is required")
    val name: String?,
    val issuingBody: String? = null,
    @field:NotNull(message = "Issue date is required")
    val issueDate: LocalDate?,
    val expiryDate: LocalDate? = null,
    val credentialId: String? = null,
    val attachmentId: java.util.UUID? = null,
)
