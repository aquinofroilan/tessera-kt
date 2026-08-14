package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CompleteInterviewRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateInterviewRequest
import com.aquinofroilan.tessera.domain.hr.dto.RescheduleInterviewRequest
import com.aquinofroilan.tessera.domain.hr.model.Interview
import com.aquinofroilan.tessera.domain.hr.model.InterviewStatus
import com.aquinofroilan.tessera.domain.hr.model.JobApplicationStatus
import com.aquinofroilan.tessera.domain.hr.repository.InterviewRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class InterviewService(
    private val interviewRepository: InterviewRepository,
    private val applicationService: JobApplicationService,
) {
    @Transactional
    fun scheduleInterview(
        request: CreateInterviewRequest,
        organizationId: UUID,
        userId: UUID,
    ): Interview {
        val scheduledAt = request.scheduledAt ?: throw BusinessRuleException("scheduledAt is required")
        val application = applicationService.getApplication(request.applicationId, organizationId)
        if (application.status == com.aquinofroilan.tessera.domain.hr.model.JobApplicationStatus.REJECTED ||
            application.status == com.aquinofroilan.tessera.domain.hr.model.JobApplicationStatus.WITHDRAWN ||
            application.status == com.aquinofroilan.tessera.domain.hr.model.JobApplicationStatus.HIRED
        ) {
            throw BusinessRuleException("Cannot schedule interview for application in terminal status: ${application.status}")
        }
        return interviewRepository.save(
            Interview(
                organizationId = organizationId,
                applicationId = application.id,
                scheduledAt = scheduledAt,
                interviewerUserId = request.interviewerUserId,
                mode = request.mode,
                notes = request.notes,
                createdBy = userId,
            ),
        )
    }

    fun getInterview(
        id: UUID,
        organizationId: UUID,
    ): Interview {
        val i =
            interviewRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Interview not found: $id")
            }
        if (i.organizationId != organizationId) {
            throw ResourceNotFoundException("Interview not found: $id")
        }
        return i
    }

    fun listInterviewsForApplication(
        organizationId: UUID,
        applicationId: UUID,
    ): List<Interview> = interviewRepository.findByOrganizationIdAndApplicationIdOrderByScheduledAtAsc(organizationId, applicationId)

    @Transactional
    fun rescheduleInterview(
        id: UUID,
        request: RescheduleInterviewRequest,
        organizationId: UUID,
    ): Interview {
        val scheduledAt = request.scheduledAt ?: throw BusinessRuleException("scheduledAt is required")
        val i = getInterview(id, organizationId)
        if (i.status != InterviewStatus.SCHEDULED) {
            throw BusinessRuleException("Only SCHEDULED interviews can be rescheduled (current: ${i.status})")
        }
        return interviewRepository.save(i.copy(scheduledAt = scheduledAt, notes = request.notes ?: i.notes))
    }

    @Transactional
    fun completeInterview(
        id: UUID,
        request: CompleteInterviewRequest,
        organizationId: UUID,
    ): Interview {
        val outcome = request.outcome ?: throw BusinessRuleException("Outcome is required")
        val i = getInterview(id, organizationId)
        if (i.status != InterviewStatus.SCHEDULED) {
            throw BusinessRuleException("Only SCHEDULED interviews can be completed (current: ${i.status})")
        }
        return interviewRepository.save(
            i.copy(
                status = InterviewStatus.COMPLETED,
                outcome = outcome,
                notes = request.notes ?: i.notes,
            ),
        )
    }

    @Transactional
    fun cancelInterview(
        id: UUID,
        organizationId: UUID,
    ): Interview {
        val i = getInterview(id, organizationId)
        if (i.status == InterviewStatus.CANCELLED) return i
        if (i.status == InterviewStatus.COMPLETED) {
            throw BusinessRuleException("Cannot cancel a COMPLETED interview")
        }
        return interviewRepository.save(i.copy(status = InterviewStatus.CANCELLED))
    }
}
