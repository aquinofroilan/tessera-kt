package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.ConvertLeadRequest
import com.aquinofroilan.tessera.dto.CreateLeadRequest
import com.aquinofroilan.tessera.dto.CreateOpportunityRequest
import com.aquinofroilan.tessera.dto.UpdateLeadRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Lead
import com.aquinofroilan.tessera.model.LeadStatus
import com.aquinofroilan.tessera.model.Opportunity
import com.aquinofroilan.tessera.model.OpportunityStatus
import com.aquinofroilan.tessera.repository.LeadRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional

class LeadServiceTest {
    private lateinit var repository: LeadRepository
    private lateinit var opportunityService: OpportunityService
    private lateinit var service: LeadService

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        repository = mock(LeadRepository::class.java)
        opportunityService = mock(OpportunityService::class.java)
        whenever(repository.save(any<Lead>())).thenAnswer { it.arguments[0] }
        service = LeadService(repository, opportunityService)
    }

    @Test
    fun `create persists a lead with NEW status`() {
        val lead = service.createLead(CreateLeadRequest(fullName = "Ada Lovelace"), orgId, userId)
        assertThat(lead.fullName).isEqualTo("Ada Lovelace")
        assertThat(lead.status).isEqualTo(LeadStatus.NEW)
    }

    @Test
    fun `update rejects setting status to CONVERTED directly`() {
        whenever(repository.findById("l1")).thenReturn(
            Optional.of(lead(LeadStatus.NEW)),
        )
        assertThatThrownBy {
            service.updateLead("l1", UpdateLeadRequest(status = LeadStatus.CONVERTED), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert flips status and stamps converted_to_opportunity_id`() {
        whenever(repository.findById("l1")).thenReturn(Optional.of(lead(LeadStatus.QUALIFIED)))
        val opp = stubOpportunity("opp-1")
        whenever(opportunityService.createOpportunity(any<CreateOpportunityRequest>(), any(), any(), any())).thenReturn(opp)

        val (updatedLead, returnedOpp) =
            service.convertLead(
                "l1",
                ConvertLeadRequest(
                    customerId = "cust-1",
                    stageId = "stage-1",
                    opportunityName = "Deal",
                    amount = BigDecimal("1000"),
                ),
                orgId,
                userId,
            )

        assertThat(updatedLead.status).isEqualTo(LeadStatus.CONVERTED)
        assertThat(updatedLead.convertedToOpportunityId).isEqualTo("opp-1")
        assertThat(returnedOpp.id).isEqualTo("opp-1")
    }

    @Test
    fun `convert rejects already-converted leads`() {
        whenever(repository.findById("l1")).thenReturn(Optional.of(lead(LeadStatus.CONVERTED).copy(convertedToOpportunityId = "opp-old")))
        assertThatThrownBy {
            service.convertLead(
                "l1",
                ConvertLeadRequest(
                    customerId = "cust-1",
                    stageId = "stage-1",
                    opportunityName = "Deal",
                    amount = BigDecimal("1000"),
                ),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    private fun lead(status: LeadStatus) =
        Lead(
            id = "l1",
            organizationId = orgId,
            fullName = "Ada",
            status = status,
            createdBy = userId,
        )

    private fun stubOpportunity(id: String) =
        Opportunity(
            id = id,
            organizationId = orgId,
            name = "Deal",
            customerId = "cust-1",
            stageId = "stage-1",
            amount = BigDecimal("1000"),
            currency = "USD",
            status = OpportunityStatus.OPEN,
            createdBy = userId,
        )
}
