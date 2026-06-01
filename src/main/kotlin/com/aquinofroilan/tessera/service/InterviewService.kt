package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CompleteInterviewRequest
import com.aquinofroilan.tessera.dto.CreateInterviewRequest
import com.aquinofroilan.tessera.dto.RescheduleInterviewRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Interview
import com.aquinofroilan.tessera.model.InterviewStatus
import com.aquinofroilan.tessera.repository.InterviewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InterviewService(
    private val interviewRepository: InterviewRepository,
    private val applicationService: JobApplicationService,
) {
    @Transactional
    fun scheduleInterview(
        request: CreateInterviewRequest,
        organizationId: String,
        userId: String,
    ): Interview {
        val scheduledAt = request.scheduledAt ?: throw BusinessRuleException("scheduledAt is required")
        val application = applicationService.getApplication(request.applicationId, organizationId)
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
        id: String,
        organizationId: String,
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
        organizationId: String,
        applicationId: String,
    ): List<Interview> = interviewRepository.findByOrganizationIdAndApplicationIdOrderByScheduledAtAsc(organizationId, applicationId)

    @Transactional
    fun rescheduleInterview(
        id: String,
        request: RescheduleInterviewRequest,
        organizationId: String,
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
        id: String,
        request: CompleteInterviewRequest,
        organizationId: String,
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
        id: String,
        organizationId: String,
    ): Interview {
        val i = getInterview(id, organizationId)
        if (i.status == InterviewStatus.CANCELLED) return i
        if (i.status == InterviewStatus.COMPLETED) {
            throw BusinessRuleException("Cannot cancel a COMPLETED interview")
        }
        return interviewRepository.save(i.copy(status = InterviewStatus.CANCELLED))
    }
}
