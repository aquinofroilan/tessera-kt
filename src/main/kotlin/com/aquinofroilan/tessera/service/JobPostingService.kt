package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateJobPostingRequest
import com.aquinofroilan.tessera.dto.UpdateJobPostingRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.JobPosting
import com.aquinofroilan.tessera.model.JobPostingStatus
import com.aquinofroilan.tessera.repository.JobPostingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class JobPostingService(
    private val jobPostingRepository: JobPostingRepository,
) {
    @Transactional
    fun createPosting(
        request: CreateJobPostingRequest,
        organizationId: UUID,
        userId: UUID,
    ): JobPosting =
        jobPostingRepository.save(
            JobPosting(
                organizationId = organizationId,
                title = request.title.trim(),
                description = request.description,
                departmentId = request.departmentId,
                positionId = request.positionId,
                ownerUserId = request.ownerUserId,
                notes = request.notes,
                createdBy = userId,
            ),
        )

    fun getPosting(
        id: UUID,
        organizationId: UUID,
    ): JobPosting {
        val p =
            jobPostingRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Job posting not found: $id")
            }
        if (p.organizationId != organizationId) {
            throw ResourceNotFoundException("Job posting not found: $id")
        }
        return p
    }

    fun listPostings(
        organizationId: UUID,
        status: JobPostingStatus?,
    ): List<JobPosting> =
        if (status != null) {
            jobPostingRepository.findByOrganizationIdAndStatus(organizationId, status)
        } else {
            jobPostingRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updatePosting(
        id: UUID,
        request: UpdateJobPostingRequest,
        organizationId: UUID,
    ): JobPosting {
        val p = getPosting(id, organizationId)
        if (p.status == JobPostingStatus.CLOSED || p.status == JobPostingStatus.CANCELLED) {
            throw BusinessRuleException("Cannot edit a ${p.status} job posting")
        }
        return jobPostingRepository.save(
            p.copy(
                title = request.title?.trim() ?: p.title,
                description = request.description ?: p.description,
                departmentId = request.departmentId ?: p.departmentId,
                positionId = request.positionId ?: p.positionId,
                ownerUserId = request.ownerUserId ?: p.ownerUserId,
                notes = request.notes ?: p.notes,
            ),
        )
    }

    @Transactional
    fun openPosting(
        id: UUID,
        organizationId: UUID,
    ): JobPosting {
        val p = getPosting(id, organizationId)
        if (p.status != JobPostingStatus.DRAFT) {
            throw BusinessRuleException("Only DRAFT postings can be opened; this is ${p.status}")
        }
        return jobPostingRepository.save(p.copy(status = JobPostingStatus.OPEN, postedAt = LocalDateTime.now()))
    }

    @Transactional
    fun closePosting(
        id: UUID,
        organizationId: UUID,
    ): JobPosting {
        val p = getPosting(id, organizationId)
        if (p.status == JobPostingStatus.CLOSED) return p
        if (p.status == JobPostingStatus.CANCELLED) {
            throw BusinessRuleException("Cannot close a CANCELLED posting")
        }
        return jobPostingRepository.save(p.copy(status = JobPostingStatus.CLOSED, closedAt = LocalDateTime.now()))
    }

    @Transactional
    fun cancelPosting(
        id: UUID,
        organizationId: UUID,
    ): JobPosting {
        val p = getPosting(id, organizationId)
        if (p.status == JobPostingStatus.CANCELLED) return p
        return jobPostingRepository.save(p.copy(status = JobPostingStatus.CANCELLED, closedAt = LocalDateTime.now()))
    }
}
