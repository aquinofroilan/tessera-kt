package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateInvoiceRequest
import com.aquinofroilan.tessera.dto.GenerateProjectInvoiceRequest
import com.aquinofroilan.tessera.dto.InvoiceLineRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Invoice
import com.aquinofroilan.tessera.model.TimeEntryStatus
import com.aquinofroilan.tessera.repository.TimeEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class ProjectBillingService(
    private val projectService: ProjectService,
    private val customerService: CustomerService,
    private val timeEntryRepository: TimeEntryRepository,
    private val invoiceService: InvoiceService,
) {
    /**
     * Generates a sales invoice from a project's approved, billable, un-billed
     * time entries (time-and-materials). Each entry becomes an invoice line at
     * hours x rate against the revenue account, and is then marked billed and
     * linked to the invoice so it cannot be billed again.
     */
    @Transactional
    fun generateInvoice(
        projectId: String,
        request: GenerateProjectInvoiceRequest,
        organizationId: String,
        createdBy: String,
    ): Invoice {
        val project = projectService.getProject(projectId, organizationId)
        val customerId =
            project.customerId ?: throw BusinessRuleException("Project has no customer to bill")
        val customer = customerService.getCustomer(customerId, organizationId)
        val revenueAccountId =
            request.revenueAccountId ?: customer.defaultRevenueAccountId
                ?: throw BusinessRuleException("No revenue account; set one on the customer or provide revenueAccountId")

        val billable =
            timeEntryRepository
                .findByOrganizationIdAndProjectIdAndStatus(organizationId, projectId, TimeEntryStatus.APPROVED)
                .filter { it.billable && !it.invoiced && (it.rate?.signum() ?: 0) > 0 }
        if (billable.isEmpty()) {
            throw BusinessRuleException("No billable time to invoice for this project")
        }

        val date = request.date ?: LocalDate.now(ZoneOffset.UTC)
        val dueDate = request.dueDate ?: date.plusDays(customer.paymentTermDays.toLong())

        val lines =
            billable.map { entry ->
                InvoiceLineRequest(
                    accountId = revenueAccountId,
                    amount = entry.hours.multiply(entry.rate!!),
                    description = "Time ${entry.entryDate} (${entry.hours}h) on ${project.projectNumber}",
                )
            }

        val invoice =
            invoiceService.createInvoice(
                CreateInvoiceRequest(
                    customerId = customerId,
                    date = date,
                    dueDate = dueDate,
                    referenceNumber = project.projectNumber,
                    currencyCode = request.currencyCode,
                    lines = lines,
                ),
                organizationId,
                createdBy,
            )

        timeEntryRepository.saveAll(billable.map { it.copy(invoiced = true, invoiceId = invoice.id) })
        return invoice
    }
}
