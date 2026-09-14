package com.aquinofroilan.tessera.domain.hr.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateAppraisalCycleRequest(
    @field:NotEmpty(message = "Name is required")
    val name: String?,
    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate?,
    @field:NotNull(message = "End date is required")
    val endDate: LocalDate?,
)

data class CreateAppraisalGoalRequest(
    @field:NotEmpty(message = "Title is required")
    val title: String?,
    val description: String? = null,
    val weight: Int = 0,
)

data class CreateAppraisalRequest(
    @field:NotNull(message = "Employee ID is required")
    val employeeId: java.util.UUID,
    val managerId: java.util.UUID? = null,
    @field:Valid
    val goals: List<CreateAppraisalGoalRequest>? = emptyList(),
)

data class UpdateAppraisalGoalRequest(
    val goalId: java.util.UUID,
    val selfRating: Int? = null,
    val managerRating: Int? = null,
    val selfComments: String? = null,
    val managerComments: String? = null,
)

data class SubmitAppraisalReviewRequest(
    @field:Valid
    val goals: List<UpdateAppraisalGoalRequest>? = emptyList(),
    val overallRating: Double? = null,
    val managerSummary: String? = null,
)
