package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateActivityRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.CrmActivity
import com.aquinofroilan.tessera.model.CrmActivityType
import com.aquinofroilan.tessera.repository.CrmActivityRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

class CrmActivityServiceTest {
    private lateinit var repository: CrmActivityRepository
    private lateinit var leadService: LeadService
    private lateinit var opportunityService: OpportunityService
    private lateinit var contactService: ContactService
    private lateinit var customerService: CustomerService
    private lateinit var service: CrmActivityService

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        repository = mock(CrmActivityRepository::class.java)
        leadService = mock(LeadService::class.java)
        opportunityService = mock(OpportunityService::class.java)
        contactService = mock(ContactService::class.java)
        customerService = mock(CustomerService::class.java)
        whenever(repository.save(any<CrmActivity>())).thenAnswer { it.arguments[0] }
        service = CrmActivityService(repository, leadService, opportunityService, contactService, customerService)
    }

    @Test
    fun `create rejects activity with no related entity`() {
        assertThatThrownBy {
            service.createActivity(
                CreateActivityRequest(type = CrmActivityType.NOTE, subject = "Random note"),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create TASK without dueAt is rejected`() {
        assertThatThrownBy {
            service.createActivity(
                CreateActivityRequest(
                    type = CrmActivityType.TASK,
                    subject = "Follow up",
                    relatedOpportunityId = "opp-1",
                ),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create persists a NOTE linked to a lead`() {
        val activity =
            service.createActivity(
                CreateActivityRequest(
                    type = CrmActivityType.NOTE,
                    subject = "Initial intro call",
                    relatedLeadId = "lead-1",
                ),
                orgId,
                userId,
            )
        assertThat(activity.type).isEqualTo(CrmActivityType.NOTE)
        assertThat(activity.relatedLeadId).isEqualTo("lead-1")
        assertThat(activity.completed).isFalse()
    }

    @Test
    fun `complete only allowed on TASK`() {
        whenever(repository.findById("a1")).thenReturn(
            Optional.of(
                CrmActivity(
                    id = "a1",
                    organizationId = orgId,
                    type = CrmActivityType.NOTE,
                    subject = "n",
                    relatedLeadId = "lead-1",
                    createdBy = userId,
                ),
            ),
        )
        assertThatThrownBy { service.completeActivity("a1", orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `complete stamps completed audit fields on TASK`() {
        whenever(repository.findById("a1")).thenReturn(
            Optional.of(
                CrmActivity(
                    id = "a1",
                    organizationId = orgId,
                    type = CrmActivityType.TASK,
                    subject = "task",
                    relatedOpportunityId = "opp-1",
                    dueAt = LocalDateTime.now().plusDays(1),
                    createdBy = userId,
                ),
            ),
        )
        val completed = service.completeActivity("a1", orgId, userId)
        assertThat(completed.completed).isTrue()
        assertThat(completed.completedBy).isEqualTo(userId)
    }
}
