package com.aquinofroilan.tessera.domain.crm.service

import com.aquinofroilan.tessera.domain.crm.dto.CloseOpportunityRequest
import com.aquinofroilan.tessera.domain.crm.dto.CreateOpportunityRequest
import com.aquinofroilan.tessera.domain.crm.model.Contact
import com.aquinofroilan.tessera.domain.crm.model.Opportunity
import com.aquinofroilan.tessera.domain.crm.model.OpportunityStatus
import com.aquinofroilan.tessera.domain.crm.model.PipelineStage
import com.aquinofroilan.tessera.domain.crm.repository.OpportunityRepository
import com.aquinofroilan.tessera.domain.organization.model.Organizations
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.domain.sales.model.Customer
import com.aquinofroilan.tessera.domain.sales.service.CustomerService
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class OpportunityServiceTest {
    private val oppId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val contactId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000006")
    private lateinit var repository: OpportunityRepository
    private lateinit var customerService: CustomerService
    private lateinit var contactService: ContactService
    private lateinit var stageService: PipelineStageService
    private lateinit var orgRepository: OrganizationRepository
    private lateinit var service: OpportunityService

    private val orgId = java.util.UUID.randomUUID()
    private val userId = java.util.UUID.randomUUID()
    private val customerId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003")
    private val openStageId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000004")
    private val wonStageId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000005")

    @BeforeEach
    fun setup() {
        repository = mock(OpportunityRepository::class.java)
        customerService = mock(CustomerService::class.java)
        contactService = mock(ContactService::class.java)
        stageService = mock(PipelineStageService::class.java)
        orgRepository = mock(OrganizationRepository::class.java)
        whenever(repository.save(any<Opportunity>())).thenAnswer { it.arguments[0] }
        whenever(customerService.getCustomer(customerId, orgId)).thenReturn(
            Customer(id = customerId, name = "Acme", organizationId = orgId, isActive = true),
        )
        whenever(stageService.getStage(openStageId, orgId)).thenReturn(stage(openStageId, "PROSPECT", isActive = true))
        whenever(stageService.getStage(wonStageId, orgId)).thenReturn(stage(wonStageId, "WON", isActive = true, isWon = true))
        whenever(orgRepository.findById(orgId)).thenReturn(
            Optional.of(
                Organizations(
                    uuid = orgId,
                    orgSlug = "org",
                    name = "Org",
                    legalName = "Org Inc.",
                    tradeName = "Org",
                    baseCurrency = "USD",
                    fiscalYearStart = java.time.LocalDateTime.now(),
                    timezone = "UTC",
                ),
            ),
        )
        service = OpportunityService(repository, customerService, contactService, stageService, orgRepository)
    }

    @Test
    fun `create defaults currency to org base currency`() {
        val opp =
            service.createOpportunity(
                CreateOpportunityRequest(
                    name = "Deal",
                    customerId = customerId,
                    stageId = openStageId,
                    amount = BigDecimal("1000"),
                ),
                orgId,
                userId,
            )
        assertThat(opp.currency).isEqualTo("USD")
        assertThat(opp.status).isEqualTo(OpportunityStatus.OPEN)
    }

    @Test
    fun `create rejects terminal stage as starting stage`() {
        assertThatThrownBy {
            service.createOpportunity(
                CreateOpportunityRequest(
                    name = "Deal",
                    customerId = customerId,
                    stageId = wonStageId,
                    amount = BigDecimal("1000"),
                ),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create rejects a contact linked to a different customer`() {
        whenever(contactService.getContact(contactId, orgId)).thenReturn(
            Contact(
                organizationId = orgId,
                customerId = java.util.UUID.randomUUID(),
                firstName = "A",
                lastName = "B",
                createdBy = userId,
            ),
        )
        assertThatThrownBy {
            service.createOpportunity(
                CreateOpportunityRequest(
                    name = "Deal",
                    customerId = customerId,
                    primaryContactId = contactId,
                    stageId = openStageId,
                    amount = BigDecimal("1000"),
                ),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `close requires a won or lost terminal stage`() {
        val open = opportunity()
        whenever(repository.findById(oppId)).thenReturn(Optional.of(open))
        // openStageId is not won/lost
        assertThatThrownBy {
            service.closeOpportunity(oppId, CloseOpportunityRequest(stageId = openStageId), orgId, userId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `close flips status to WON when stage is won`() {
        val open = opportunity()
        whenever(repository.findById(oppId)).thenReturn(Optional.of(open))
        val closed = service.closeOpportunity(oppId, CloseOpportunityRequest(stageId = wonStageId), orgId, userId)
        assertThat(closed.status).isEqualTo(OpportunityStatus.WON)
        assertThat(closed.closedBy).isEqualTo(userId)
    }

    private fun opportunity() =
        Opportunity(
            id = oppId,
            organizationId = orgId,
            name = "Deal",
            customerId = customerId,
            stageId = openStageId,
            amount = BigDecimal("1000"),
            currency = "USD",
            status = OpportunityStatus.OPEN,
            createdBy = userId,
        )

    private fun stage(
        id: java.util.UUID,
        code: String,
        isActive: Boolean,
        isWon: Boolean = false,
        isLost: Boolean = false,
    ) = PipelineStage(
        id = id,
        organizationId = orgId,
        code = code,
        name = code,
        sortOrder = 10,
        isActive = isActive,
        isWon = isWon,
        isLost = isLost,
    )
}
