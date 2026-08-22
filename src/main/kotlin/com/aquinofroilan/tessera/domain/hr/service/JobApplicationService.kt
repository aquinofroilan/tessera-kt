package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.AdvanceApplicationRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateApplicationRequest
import com.aquinofroilan.tessera.domain.hr.dto.UpdateApplicationRequest
import com.aquinofroilan.tessera.domain.hr.model.JobApplication
import com.aquinofroilan.tessera.domain.hr.model.JobApplicationStatus
import com.aquinofroilan.tessera.domain.hr.model.JobPostingStatus
import com.aquinofroilan.tessera.domain.hr.repository.JobApplicationRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class JobApplicationService(
    private val applicationRepository: JobApplicationRepository,
    private val jobPostingService: JobPostingService,
) {
    @Transactional
    fun createApplication(
        request: CreateApplicationRequest,
        organizationId: UUID,
        userId: UUID,
    ): JobApplication {
        val posting = jobPostingService.getPosting(request.jobPostingId, organizationId)
        if (posting.status != JobPostingStatus.OPEN) {
            throw BusinessRuleException("Applications can only be created for OPEN job postings (current: ${posting.status})")
        }
        return applicationRepository.save(
            JobApplication(
                organizationId = organizationId,
                jobPostingId = posting.id,
                candidateFullName = request.candidateFullName.trim(),
                email = request.email?.trim(),
                phone = request.phone?.trim(),
                resumeUrl = request.resumeUrl,
                source = request.source,
                ownerUserId = request.ownerUserId,
                notes = request.notes,
                createdBy = userId,
            ),
        )
    }

    fun getApplication(
        id: UUID,
        organizationId: UUID,
    ): JobApplication {
        val a =
            applicationRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Application not found: $id")
            }
        if (a.organizationId != organizationId) {
            throw ResourceNotFoundException("Application not found: $id")
        }
        return a
    }

    fun listApplications(
        organizationId: UUID,
        status: JobApplicationStatus?,
        jobPostingId: UUID?,
    ): List<JobApplication> =
        when {
            jobPostingId != null ->
                applicationRepository.findByOrganizationIdAndJobPostingId(organizationId, jobPostingId)
            status != null ->
                applicationRepository.findByOrganizationIdAndStatus(organizationId, status)
            else ->
                applicationRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateApplication(
        id: UUID,
        request: UpdateApplicationRequest,
        organizationId: UUID,
    ): JobApplication {
        val a = getApplication(id, organizationId)
        if (isTerminal(a.status)) {
            throw BusinessRuleException("Cannot edit a ${a.status} application")
        }
        return applicationRepository.save(
            a.copy(
                candidateFullName = request.candidateFullName?.trim() ?: a.candidateFullName,
                email = request.email?.trim() ?: a.email,
                phone = request.phone?.trim() ?: a.phone,
                resumeUrl = request.resumeUrl ?: a.resumeUrl,
                source = request.source ?: a.source,
                ownerUserId = request.ownerUserId ?: a.ownerUserId,
                notes = request.notes ?: a.notes,
            ),
        )
    }

    @Transactional
    fun advanceApplication(
        id: UUID,
        request: AdvanceApplicationRequest,
        organizationId: UUID,
    ): JobApplication {
        val target = request.status ?: throw BusinessRuleException("Target status is required")
        val a = getApplication(id, organizationId)
        if (isTerminal(a.status)) {
            throw BusinessRuleException("Application is already ${a.status}")
        }
        if (!allowedTransition(a.status, target)) {
            throw BusinessRuleException("Illegal transition: ${a.status} -> $target")
        }
        return applicationRepository.save(
            a.copy(
                status = target,
                notes = request.notes ?: a.notes,
            ),
        )
    }

    private fun isTerminal(status: JobApplicationStatus): Boolean =
        status == JobApplicationStatus.HIRED ||
            status == JobApplicationStatus.REJECTED ||
            status == JobApplicationStatus.WITHDRAWN

    private fun allowedTransition(
        from: JobApplicationStatus,
        to: JobApplicationStatus,
    ): Boolean {
        if (to == JobApplicationStatus.REJECTED || to == JobApplicationStatus.WITHDRAWN) return true
        val forward =
            listOf(
                JobApplicationStatus.APPLIED,
                JobApplicationStatus.SCREENING,
                JobApplicationStatus.INTERVIEW,
                JobApplicationStatus.OFFERED,
                JobApplicationStatus.HIRED,
            )
        val fromIdx = forward.indexOf(from)
        val toIdx = forward.indexOf(to)
        return fromIdx >= 0 && toIdx == fromIdx + 1
    }
}
