package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateJobPostingRequest
import com.aquinofroilan.tessera.dto.UpdateJobPostingRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.JobPosting
import com.aquinofroilan.tessera.model.JobPostingStatus
import com.aquinofroilan.tessera.repository.JobPostingRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional

class JobPostingServiceTest {
    private lateinit var repository: JobPostingRepository
    private lateinit var service: JobPostingService

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        repository = mock(JobPostingRepository::class.java)
        whenever(repository.save(any<JobPosting>())).thenAnswer { it.arguments[0] }
        service = JobPostingService(repository)
    }

    @Test
    fun `create persists a DRAFT posting`() {
        val p = service.createPosting(CreateJobPostingRequest(title = " Engineer "), orgId, userId)
        assertThat(p.title).isEqualTo("Engineer")
        assertThat(p.status).isEqualTo(JobPostingStatus.DRAFT)
    }

    @Test
    fun `open is rejected for non-DRAFT`() {
        whenever(repository.findById("p1")).thenReturn(Optional.of(posting().copy(status = JobPostingStatus.OPEN)))
        assertThatThrownBy { service.openPosting("p1", orgId) }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `open flips to OPEN and stamps postedAt`() {
        whenever(repository.findById("p1")).thenReturn(Optional.of(posting()))
        val opened = service.openPosting("p1", orgId)
        assertThat(opened.status).isEqualTo(JobPostingStatus.OPEN)
        assertThat(opened.postedAt).isNotNull
    }

    @Test
    fun `update rejected when CLOSED`() {
        whenever(repository.findById("p1")).thenReturn(Optional.of(posting().copy(status = JobPostingStatus.CLOSED)))
        assertThatThrownBy {
            service.updatePosting("p1", UpdateJobPostingRequest(title = "x"), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    private fun posting() =
        JobPosting(
            id = "p1",
            organizationId = orgId,
            title = "Engineer",
            status = JobPostingStatus.DRAFT,
            createdBy = userId,
        )
}
