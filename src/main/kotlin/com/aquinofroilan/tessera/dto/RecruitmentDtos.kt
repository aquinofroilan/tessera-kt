package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.Interview
import com.aquinofroilan.tessera.model.InterviewMode
import com.aquinofroilan.tessera.model.InterviewOutcome
import com.aquinofroilan.tessera.model.InterviewStatus
import com.aquinofroilan.tessera.model.JobApplication
import com.aquinofroilan.tessera.model.JobApplicationStatus
import com.aquinofroilan.tessera.model.JobPosting
import com.aquinofroilan.tessera.model.JobPostingStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreateJobPostingRequest(
    @field:NotBlank(message = "Title is required")
    val title: String,
    val description: String? = null,
    val departmentId: String? = null,
    val positionId: String? = null,
    val ownerUserId: String? = null,
    val notes: String? = null,
)

data class UpdateJobPostingRequest(
    val title: String? = null,
    val description: String? = null,
    val departmentId: String? = null,
    val positionId: String? = null,
    val ownerUserId: String? = null,
    val notes: String? = null,
)

data class JobPostingResponse(
    val id: String,
    val title: String,
    val description: String?,
    val departmentId: String?,
    val positionId: String?,
    val status: JobPostingStatus,
    val postedAt: LocalDateTime?,
    val closedAt: LocalDateTime?,
    val ownerUserId: String?,
    val notes: String?,
) {
    companion object {
        fun from(j: JobPosting) =
            JobPostingResponse(
                id = j.id,
                title = j.title,
                description = j.description,
                departmentId = j.departmentId,
                positionId = j.positionId,
                status = j.status,
                postedAt = j.postedAt,
                closedAt = j.closedAt,
                ownerUserId = j.ownerUserId,
                notes = j.notes,
            )
    }
}

data class CreateApplicationRequest(
    @field:NotBlank(message = "Job posting ID is required")
    val jobPostingId: String,
    @field:NotBlank(message = "Candidate name is required")
    val candidateFullName: String,
    @field:Email
    val email: String? = null,
    val phone: String? = null,
    val resumeUrl: String? = null,
    val source: String? = null,
    val ownerUserId: String? = null,
    val notes: String? = null,
)

data class UpdateApplicationRequest(
    val candidateFullName: String? = null,
    @field:Email
    val email: String? = null,
    val phone: String? = null,
    val resumeUrl: String? = null,
    val source: String? = null,
    val ownerUserId: String? = null,
    val notes: String? = null,
)

data class AdvanceApplicationRequest(
    @field:NotNull(message = "Target status is required")
    val status: JobApplicationStatus?,
    val notes: String? = null,
)

data class ApplicationResponse(
    val id: String,
    val jobPostingId: String,
    val candidateFullName: String,
    val email: String?,
    val phone: String?,
    val resumeUrl: String?,
    val source: String?,
    val status: JobApplicationStatus,
    val appliedAt: LocalDateTime,
    val ownerUserId: String?,
    val notes: String?,
) {
    companion object {
        fun from(a: JobApplication) =
            ApplicationResponse(
                id = a.id,
                jobPostingId = a.jobPostingId,
                candidateFullName = a.candidateFullName,
                email = a.email,
                phone = a.phone,
                resumeUrl = a.resumeUrl,
                source = a.source,
                status = a.status,
                appliedAt = a.appliedAt,
                ownerUserId = a.ownerUserId,
                notes = a.notes,
            )
    }
}

data class CreateInterviewRequest(
    @field:NotBlank(message = "Application ID is required")
    val applicationId: String,
    @field:NotNull(message = "Scheduled timestamp is required")
    val scheduledAt: LocalDateTime?,
    val interviewerUserId: String? = null,
    val mode: InterviewMode = InterviewMode.VIDEO,
    val notes: String? = null,
)

data class RescheduleInterviewRequest(
    @field:NotNull
    val scheduledAt: LocalDateTime?,
    val notes: String? = null,
)

data class CompleteInterviewRequest(
    @field:NotNull(message = "Outcome is required")
    val outcome: InterviewOutcome?,
    val notes: String? = null,
)

data class InterviewResponse(
    val id: String,
    val applicationId: String,
    val scheduledAt: LocalDateTime,
    val interviewerUserId: String?,
    val mode: InterviewMode,
    val status: InterviewStatus,
    val outcome: InterviewOutcome?,
    val notes: String?,
) {
    companion object {
        fun from(i: Interview) =
            InterviewResponse(
                id = i.id,
                applicationId = i.applicationId,
                scheduledAt = i.scheduledAt,
                interviewerUserId = i.interviewerUserId,
                mode = i.mode,
                status = i.status,
                outcome = i.outcome,
                notes = i.notes,
            )
    }
}
