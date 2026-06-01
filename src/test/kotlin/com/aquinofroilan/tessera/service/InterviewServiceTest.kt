package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CompleteInterviewRequest
import com.aquinofroilan.tessera.dto.CreateInterviewRequest
import com.aquinofroilan.tessera.dto.RescheduleInterviewRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Interview
import com.aquinofroilan.tessera.model.InterviewMode
import com.aquinofroilan.tessera.model.InterviewOutcome
import com.aquinofroilan.tessera.model.InterviewStatus
import com.aquinofroilan.tessera.model.JobApplication
import com.aquinofroilan.tessera.model.JobApplicationStatus
import com.aquinofroilan.tessera.repository.InterviewRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

class InterviewServiceTest {
    private lateinit var repository: InterviewRepository
    private lateinit var applicationService: JobApplicationService
    private lateinit var service: InterviewService

    private val orgId = "org-1"
    private val userId = "user-1"
    private val appId = "a1"

    @BeforeEach
    fun setup() {
        repository = mock(InterviewRepository::class.java)
        applicationService = mock(JobApplicationService::class.java)
        whenever(repository.save(any<Interview>())).thenAnswer { it.arguments[0] }
        whenever(applicationService.getApplication(appId, orgId)).thenReturn(application())
        service = InterviewService(repository, applicationService)
    }

    @Test
    fun `schedule persists with SCHEDULED status`() {
        val i =
            service.scheduleInterview(
                CreateInterviewRequest(
                    applicationId = appId,
                    scheduledAt = LocalDateTime.now().plusDays(1),
                    mode = InterviewMode.PHONE,
                ),
                orgId,
                userId,
            )
        assertThat(i.status).isEqualTo(InterviewStatus.SCHEDULED)
        assertThat(i.mode).isEqualTo(InterviewMode.PHONE)
    }

    @Test
    fun `complete sets outcome and flips status`() {
        whenever(repository.findById("i1")).thenReturn(Optional.of(scheduled()))
        val done =
            service.completeInterview(
                "i1",
                CompleteInterviewRequest(outcome = InterviewOutcome.PASS),
                orgId,
            )
        assertThat(done.status).isEqualTo(InterviewStatus.COMPLETED)
        assertThat(done.outcome).isEqualTo(InterviewOutcome.PASS)
    }

    @Test
    fun `complete rejects on already-COMPLETED`() {
        whenever(repository.findById("i1")).thenReturn(Optional.of(scheduled().copy(status = InterviewStatus.COMPLETED)))
        assertThatThrownBy {
            service.completeInterview("i1", CompleteInterviewRequest(outcome = InterviewOutcome.PASS), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `reschedule rejects when not SCHEDULED`() {
        whenever(repository.findById("i1")).thenReturn(Optional.of(scheduled().copy(status = InterviewStatus.COMPLETED)))
        assertThatThrownBy {
            service.rescheduleInterview("i1", RescheduleInterviewRequest(scheduledAt = LocalDateTime.now().plusDays(2)), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `cancel rejects COMPLETED`() {
        whenever(repository.findById("i1")).thenReturn(Optional.of(scheduled().copy(status = InterviewStatus.COMPLETED)))
        assertThatThrownBy { service.cancelInterview("i1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    private fun application() =
        JobApplication(
            id = appId,
            organizationId = orgId,
            jobPostingId = "p1",
            candidateFullName = "Ada",
            status = JobApplicationStatus.INTERVIEW,
            createdBy = userId,
        )

    private fun scheduled() =
        Interview(
            id = "i1",
            organizationId = orgId,
            applicationId = appId,
            scheduledAt = LocalDateTime.now().plusDays(1),
            createdBy = userId,
        )
}
