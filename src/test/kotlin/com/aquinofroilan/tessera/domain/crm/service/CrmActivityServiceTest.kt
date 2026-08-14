package com.aquinofroilan.tessera.domain.crm.service

import com.aquinofroilan.tessera.domain.crm.dto.CreateActivityRequest
import com.aquinofroilan.tessera.domain.crm.model.CrmActivity
import com.aquinofroilan.tessera.domain.crm.model.CrmActivityType
import com.aquinofroilan.tessera.domain.crm.repository.CrmActivityRepository
import com.aquinofroilan.tessera.domain.sales.service.CustomerService
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class CrmActivityServiceTest {
    private lateinit var repository: CrmActivityRepository
    private lateinit var leadService: LeadService
    private lateinit var opportunityService: OpportunityService
    private lateinit var contactService: ContactService
    private lateinit var customerService: CustomerService
    private lateinit var service: CrmActivityService

    private val orgId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000002")

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
                    relatedOpportunityId = UUID.fromString("00000000-0000-0000-0000-000000000004"),
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
                    relatedLeadId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                ),
                orgId,
                userId,
            )
        assertThat(activity.type).isEqualTo(CrmActivityType.NOTE)
        assertThat(activity.relatedLeadId).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000003"))
        assertThat(activity.completed).isFalse()
    }

    @Test
    fun `complete only allowed on TASK`() {
        whenever(repository.findById(UUID.fromString("00000000-0000-0000-0000-000000000005"))).thenReturn(
            Optional.of(
                CrmActivity(
                    id = UUID.fromString("00000000-0000-0000-0000-000000000005"),
                    organizationId = orgId,
                    type = CrmActivityType.NOTE,
                    subject = "n",
                    relatedLeadId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                    createdBy = userId,
                ),
            ),
        )
        assertThatThrownBy { service.completeActivity(UUID.fromString("00000000-0000-0000-0000-000000000005"), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `complete stamps completed audit fields on TASK`() {
        whenever(repository.findById(UUID.fromString("00000000-0000-0000-0000-000000000005"))).thenReturn(
            Optional.of(
                CrmActivity(
                    id = UUID.fromString("00000000-0000-0000-0000-000000000005"),
                    organizationId = orgId,
                    type = CrmActivityType.TASK,
                    subject = "task",
                    relatedOpportunityId = UUID.fromString("00000000-0000-0000-0000-000000000004"),
                    dueAt = LocalDateTime.now().plusDays(1),
                    createdBy = userId,
                ),
            ),
        )
        val completed = service.completeActivity(UUID.fromString("00000000-0000-0000-0000-000000000005"), orgId, userId)
        assertThat(completed.completed).isTrue()
        assertThat(completed.completedBy).isEqualTo(userId)
    }
}
