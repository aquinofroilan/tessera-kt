package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateJobPostingRequest
import com.aquinofroilan.tessera.domain.hr.dto.UpdateJobPostingRequest
import com.aquinofroilan.tessera.domain.hr.model.JobPosting
import com.aquinofroilan.tessera.domain.hr.model.JobPostingStatus
import com.aquinofroilan.tessera.domain.hr.repository.JobPostingRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

class JobPostingServiceTest {
    private lateinit var repository: JobPostingRepository
    private lateinit var service: JobPostingService

    private val orgId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000002")

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
        whenever(
            repository.findById(UUID.fromString("00000000-0000-0000-0000-000000000010")),
        ).thenReturn(Optional.of(posting().copy(status = JobPostingStatus.OPEN)))
        assertThatThrownBy {
            service.openPosting(UUID.fromString("00000000-0000-0000-0000-000000000010"), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `open flips to OPEN and stamps postedAt`() {
        whenever(repository.findById(UUID.fromString("00000000-0000-0000-0000-000000000010"))).thenReturn(Optional.of(posting()))
        val opened = service.openPosting(UUID.fromString("00000000-0000-0000-0000-000000000010"), orgId)
        assertThat(opened.status).isEqualTo(JobPostingStatus.OPEN)
        assertThat(opened.postedAt).isNotNull
    }

    @Test
    fun `update rejected when CLOSED`() {
        whenever(
            repository.findById(UUID.fromString("00000000-0000-0000-0000-000000000010")),
        ).thenReturn(Optional.of(posting().copy(status = JobPostingStatus.CLOSED)))
        assertThatThrownBy {
            service.updatePosting(UUID.fromString("00000000-0000-0000-0000-000000000010"), UpdateJobPostingRequest(title = "x"), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    private fun posting() =
        JobPosting(
            id = UUID.fromString("00000000-0000-0000-0000-000000000010"),
            organizationId = orgId,
            title = "Engineer",
            status = JobPostingStatus.DRAFT,
            createdBy = userId,
        )
}
