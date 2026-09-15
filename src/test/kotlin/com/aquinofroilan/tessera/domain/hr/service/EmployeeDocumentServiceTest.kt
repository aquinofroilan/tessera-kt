package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.AttachEmployeeDocumentRequest
import com.aquinofroilan.tessera.domain.hr.model.DocumentCategory
import com.aquinofroilan.tessera.domain.hr.model.Employee
import com.aquinofroilan.tessera.domain.hr.model.EmployeeDocument
import com.aquinofroilan.tessera.domain.hr.model.EmploymentStatus
import com.aquinofroilan.tessera.domain.hr.repository.EmployeeDocumentRepository
import com.aquinofroilan.tessera.domain.platform.model.Attachment
import com.aquinofroilan.tessera.domain.platform.service.AttachmentService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

class EmployeeDocumentServiceTest {
    private val employeeDocumentRepository: EmployeeDocumentRepository = mock()
    private val employeeService: EmployeeService = mock()
    private val attachmentService: AttachmentService = mock()

    private val service =
        EmployeeDocumentService(
            employeeDocumentRepository,
            employeeService,
            attachmentService,
        )

    private val orgId = UUID.randomUUID()
    private val employeeId = UUID.randomUUID()
    private val attachmentId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        val mockEmployee =
            Employee(
                id = employeeId,
                employeeNumber = "E004",
                firstName = "Document",
                lastName = "Tester",
                hireDate = LocalDate.now(),
                organizationId = orgId,
                status = EmploymentStatus.ACTIVE,
            )
        whenever(employeeService.getEmployee(employeeId, orgId)).thenReturn(mockEmployee)

        val mockAttachment =
            Attachment(
                id = attachmentId,
                organizationId = orgId,
                entityType = "EMPLOYEE",
                entityId = employeeId,
                filename = "contract.pdf",
                mimeType = "application/pdf",
                sizeBytes = 1024,
                storageKey = "contracts/contract.pdf",
                uploadedBy = UUID.randomUUID(),
            )
        whenever(attachmentService.getAttachment(attachmentId, orgId)).thenReturn(mockAttachment)
        whenever(employeeDocumentRepository.save(any<EmployeeDocument>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `attachDocument successfully binds an attachment as an employee document`() {
        val request =
            AttachEmployeeDocumentRequest(
                attachmentId = attachmentId,
                category = DocumentCategory.CONTRACT,
                expiryDate = LocalDate.now().plusYears(1),
            )
        val doc = service.attachDocument(employeeId, request, orgId)

        assertThat(doc.employeeId).isEqualTo(employeeId)
        assertThat(doc.attachmentId).isEqualTo(attachmentId)
        assertThat(doc.category).isEqualTo(DocumentCategory.CONTRACT)
        assertThat(doc.expiryDate).isNotNull
    }
}
