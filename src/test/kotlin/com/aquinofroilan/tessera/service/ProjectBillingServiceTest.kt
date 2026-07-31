package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateInvoiceRequest
import com.aquinofroilan.tessera.dto.GenerateProjectInvoiceRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Customer
import com.aquinofroilan.tessera.model.Invoice
import com.aquinofroilan.tessera.model.Project
import com.aquinofroilan.tessera.model.TimeEntry
import com.aquinofroilan.tessera.model.TimeEntryStatus
import com.aquinofroilan.tessera.repository.TimeEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class ProjectBillingServiceTest {
    private lateinit var projectService: ProjectService
    private lateinit var customerService: CustomerService
    private lateinit var timeEntryRepository: TimeEntryRepository
    private lateinit var invoiceService: InvoiceService
    private lateinit var service: ProjectBillingService

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val projectId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89")
    private val userId  = java.util.UUID.fromString("1fd9446c-9f04-31e6-941e-53b391d01cab")
    private val day = LocalDate.of(2026, 5, 1)

    @BeforeEach
    fun setup() {
        projectService = mock(ProjectService::class.java)
        customerService = mock(CustomerService::class.java)
        timeEntryRepository = mock(TimeEntryRepository::class.java)
        invoiceService = mock(InvoiceService::class.java)
        whenever(
            projectService.getProject(projectId, orgId),
        ).thenReturn(project(customerId = java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230")))
        whenever(customerService.getCustomer(java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"), orgId)).thenReturn(
            Customer(
                id = java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"),
                name = "Globex",
                paymentTermDays = 30,
                defaultRevenueAccountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                organizationId = orgId,
            ),
        )
        val invoice = mock(Invoice::class.java)
        whenever(invoice.id).thenReturn(java.util.UUID.fromString("feba952a-78ae-3408-9880-1611b3e9e0b5"))
        whenever(invoiceService.createInvoice(any(), eq(orgId), eq(userId))).thenReturn(invoice)
        service = ProjectBillingService(projectService, customerService, timeEntryRepository, invoiceService)
    }

    private fun project(customerId: UUID?) =
        Project(
            id = projectId,
            projectNumber = "PRJ-0001",
            name = "Apollo",
            startDate = day,
            customerId = customerId,
            organizationId = orgId,
        )

    private fun entry(
        rate: String?,
        billable: Boolean = true,
        invoiced: Boolean = false,
    ) = TimeEntry(
        id = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
        employeeId = java.util.UUID.fromString("00262aa5-14d7-3a01-b098-d7e370f001b2"),
        projectId = projectId,
        entryDate = day,
        hours = BigDecimal("4"),
        billable = billable,
        rate = rate?.let { BigDecimal(it) },
        status = TimeEntryStatus.APPROVED,
        invoiced = invoiced,
        organizationId = orgId,
    )

    @Test
    fun `generate invoice bills approved billable time and marks it invoiced`() {
        whenever(timeEntryRepository.findByOrganizationIdAndProjectIdAndStatus(orgId, projectId, TimeEntryStatus.APPROVED))
            .thenReturn(listOf(entry("50"), entry("50"), entry(null), entry("50", billable = false), entry("50", invoiced = true)))

        val invoice = service.generateInvoice(projectId, GenerateProjectInvoiceRequest(), orgId, userId)

        assertThat(invoice.id).isEqualTo(java.util.UUID.fromString("feba952a-78ae-3408-9880-1611b3e9e0b5"))
        val captor = argumentCaptor<CreateInvoiceRequest>()
        verify(invoiceService).createInvoice(captor.capture(), eq(orgId), eq(userId))
        // Only the two billable, un-invoiced, rated entries are billed.
        assertThat(captor.firstValue.lines).hasSize(2)
        assertThat(captor.firstValue.lines[0].accountId).isEqualTo(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))
        assertThat(captor.firstValue.lines[0].amount).isEqualByComparingTo("200")
        assertThat(captor.firstValue.customerId).isEqualTo(java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"))

        val saved = argumentCaptor<List<TimeEntry>>()
        verify(timeEntryRepository).saveAll(saved.capture())
        assertThat(saved.firstValue).hasSize(2)
        assertThat(saved.firstValue).allMatch {
            it.invoiced &&
                it.invoiceId == java.util.UUID.fromString("feba952a-78ae-3408-9880-1611b3e9e0b5")
        }
    }

    @Test
    fun `generate invoice fails when the project has no customer`() {
        whenever(projectService.getProject(projectId, orgId)).thenReturn(project(customerId = null))
        assertThatThrownBy { service.generateInvoice(projectId, GenerateProjectInvoiceRequest(), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `generate invoice fails when no revenue account can be resolved`() {
        whenever(customerService.getCustomer(java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"), orgId))
            .thenReturn(
                Customer(
                    id = java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"),
                    name = "Globex",
                    defaultRevenueAccountId = null,
                    organizationId = orgId,
                ),
            )
        whenever(timeEntryRepository.findByOrganizationIdAndProjectIdAndStatus(orgId, projectId, TimeEntryStatus.APPROVED))
            .thenReturn(listOf(entry("50")))

        assertThatThrownBy { service.generateInvoice(projectId, GenerateProjectInvoiceRequest(), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `generate invoice fails when there is no billable time`() {
        whenever(timeEntryRepository.findByOrganizationIdAndProjectIdAndStatus(orgId, projectId, TimeEntryStatus.APPROVED))
            .thenReturn(listOf(entry(null), entry("50", invoiced = true)))

        assertThatThrownBy { service.generateInvoice(projectId, GenerateProjectInvoiceRequest(), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }
}
