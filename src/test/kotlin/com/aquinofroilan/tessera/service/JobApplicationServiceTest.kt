package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.AdvanceApplicationRequest
import com.aquinofroilan.tessera.dto.CreateApplicationRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.JobApplication
import com.aquinofroilan.tessera.model.JobApplicationStatus
import com.aquinofroilan.tessera.model.JobPosting
import com.aquinofroilan.tessera.model.JobPostingStatus
import com.aquinofroilan.tessera.repository.JobApplicationRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

class JobApplicationServiceTest {
    private lateinit var repository: JobApplicationRepository
    private lateinit var postingService: JobPostingService
    private lateinit var service: JobApplicationService

    private val orgId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val postingId = UUID.fromString("00000000-0000-0000-0000-000000000010")

    @BeforeEach
    fun setup() {
        repository = mock(JobApplicationRepository::class.java)
        postingService = mock(JobPostingService::class.java)
        whenever(repository.save(any<JobApplication>())).thenAnswer { it.arguments[0] }
        whenever(postingService.getPosting(postingId, orgId)).thenReturn(openPosting())
        service = JobApplicationService(repository, postingService)
    }

    @Test
    fun `create only allowed on OPEN postings`() {
        whenever(postingService.getPosting(postingId, orgId)).thenReturn(openPosting().copy(status = JobPostingStatus.DRAFT))
        assertThatThrownBy {
            service.createApplication(
                CreateApplicationRequest(jobPostingId = postingId, candidateFullName = "Ada"),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `advance follows the linear pipeline`() {
        whenever(
            repository.findById(UUID.fromString("00000000-0000-0000-0000-000000000011")),
        ).thenReturn(Optional.of(application(JobApplicationStatus.APPLIED)))
        val next =
            service.advanceApplication(
                UUID.fromString("00000000-0000-0000-0000-000000000011"),
                AdvanceApplicationRequest(status = JobApplicationStatus.SCREENING),
                orgId,
            )
        assertThat(next.status).isEqualTo(JobApplicationStatus.SCREENING)
    }

    @Test
    fun `advance rejects skipping a stage`() {
        whenever(
            repository.findById(UUID.fromString("00000000-0000-0000-0000-000000000011")),
        ).thenReturn(Optional.of(application(JobApplicationStatus.APPLIED)))
        assertThatThrownBy {
            service.advanceApplication(
                UUID.fromString("00000000-0000-0000-0000-000000000011"),
                AdvanceApplicationRequest(status = JobApplicationStatus.INTERVIEW),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `advance to REJECTED is allowed from any stage`() {
        whenever(
            repository.findById(UUID.fromString("00000000-0000-0000-0000-000000000011")),
        ).thenReturn(Optional.of(application(JobApplicationStatus.SCREENING)))
        val rejected =
            service.advanceApplication(
                UUID.fromString("00000000-0000-0000-0000-000000000011"),
                AdvanceApplicationRequest(status = JobApplicationStatus.REJECTED),
                orgId,
            )
        assertThat(rejected.status).isEqualTo(JobApplicationStatus.REJECTED)
    }

    @Test
    fun `advance is rejected once terminal`() {
        whenever(
            repository.findById(UUID.fromString("00000000-0000-0000-0000-000000000011")),
        ).thenReturn(Optional.of(application(JobApplicationStatus.HIRED)))
        assertThatThrownBy {
            service.advanceApplication(
                UUID.fromString("00000000-0000-0000-0000-000000000011"),
                AdvanceApplicationRequest(status = JobApplicationStatus.REJECTED),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    private fun openPosting() =
        JobPosting(id = postingId, organizationId = orgId, title = "Engineer", status = JobPostingStatus.OPEN, createdBy = userId)

    private fun application(status: JobApplicationStatus) =
        JobApplication(
            id = UUID.fromString("00000000-0000-0000-0000-000000000011"),
            organizationId = orgId,
            jobPostingId = postingId,
            candidateFullName = "Ada Lovelace",
            status = status,
            createdBy = userId,
        )
}
