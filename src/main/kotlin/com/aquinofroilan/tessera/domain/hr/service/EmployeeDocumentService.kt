package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.AttachEmployeeDocumentRequest
import com.aquinofroilan.tessera.domain.hr.model.EmployeeDocument
import com.aquinofroilan.tessera.domain.hr.repository.EmployeeDocumentRepository
import com.aquinofroilan.tessera.domain.platform.service.AttachmentService
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class EmployeeDocumentService(
    private val employeeDocumentRepository: EmployeeDocumentRepository,
    private val employeeService: EmployeeService,
    private val attachmentService: AttachmentService,
) {
    @Transactional
    fun attachDocument(
        employeeId: UUID,
        request: AttachEmployeeDocumentRequest,
        organizationId: UUID,
    ): EmployeeDocument {
        val emp = employeeService.getEmployee(employeeId, organizationId)

        val attachId = request.attachmentId ?: throw BusinessRuleException("Attachment ID is required")
        val category = request.category ?: throw BusinessRuleException("Category is required")

        // Ensure the attachment belongs to the organization and exists
        attachmentService.getAttachment(attachId, organizationId)

        val doc =
            EmployeeDocument(
                organizationId = organizationId,
                employeeId = employeeId,
                attachmentId = attachId,
                category = category,
                expiryDate = request.expiryDate,
            )
        return employeeDocumentRepository.save(doc)
    }

    fun getEmployeeDocuments(
        employeeId: UUID,
        organizationId: UUID,
    ): List<EmployeeDocument> = employeeDocumentRepository.findByOrganizationIdAndEmployeeId(organizationId, employeeId)

    fun getExpiringDocuments(
        organizationId: UUID,
        daysToLookAhead: Long = 30,
    ): List<EmployeeDocument> {
        val now = LocalDate.now()
        val endDate = now.plusDays(daysToLookAhead)
        return employeeDocumentRepository.findByOrganizationIdAndExpiryDateBetween(organizationId, now, endDate)
    }
}
