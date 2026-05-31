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

    private val orgId = "org-1"
    private val projectId = "p-1"
    private val userId = "u-1"
    private val day = LocalDate.of(2026, 5, 1)

    @BeforeEach
    fun setup() {
        projectService = mock(ProjectService::class.java)
        customerService = mock(CustomerService::class.java)
        timeEntryRepository = mock(TimeEntryRepository::class.java)
        invoiceService = mock(InvoiceService::class.java)
        whenever(projectService.getProject(projectId, orgId)).thenReturn(project(customerId = "c-1"))
        whenever(customerService.getCustomer("c-1", orgId)).thenReturn(
            Customer(id = "c-1", name = "Globex", paymentTermDays = 30, defaultRevenueAccountId = "acc-1", organizationId = orgId),
        )
        val invoice = mock(Invoice::class.java)
        whenever(invoice.id).thenReturn("inv-1")
        whenever(invoiceService.createInvoice(any(), eq(orgId), eq(userId))).thenReturn(invoice)
        service = ProjectBillingService(projectService, customerService, timeEntryRepository, invoiceService)
    }

    private fun project(customerId: String?) =
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
        id = UUID.randomUUID().toString(),
        employeeId = "e-1",
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

        assertThat(invoice.id).isEqualTo("inv-1")
        val captor = argumentCaptor<CreateInvoiceRequest>()
        verify(invoiceService).createInvoice(captor.capture(), eq(orgId), eq(userId))
        // Only the two billable, un-invoiced, rated entries are billed.
        assertThat(captor.firstValue.lines).hasSize(2)
        assertThat(captor.firstValue.lines[0].accountId).isEqualTo("acc-1")
        assertThat(captor.firstValue.lines[0].amount).isEqualByComparingTo("200")
        assertThat(captor.firstValue.customerId).isEqualTo("c-1")

        val saved = argumentCaptor<List<TimeEntry>>()
        verify(timeEntryRepository).saveAll(saved.capture())
        assertThat(saved.firstValue).hasSize(2)
        assertThat(saved.firstValue).allMatch { it.invoiced && it.invoiceId == "inv-1" }
    }

    @Test
    fun `generate invoice fails when the project has no customer`() {
        whenever(projectService.getProject(projectId, orgId)).thenReturn(project(customerId = null))
        assertThatThrownBy { service.generateInvoice(projectId, GenerateProjectInvoiceRequest(), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `generate invoice fails when no revenue account can be resolved`() {
        whenever(customerService.getCustomer("c-1", orgId))
            .thenReturn(Customer(id = "c-1", name = "Globex", defaultRevenueAccountId = null, organizationId = orgId))
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
