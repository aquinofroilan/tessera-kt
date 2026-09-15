package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.ChecklistTaskTemplateDto
import com.aquinofroilan.tessera.domain.hr.dto.CreateChecklistTemplateRequest
import com.aquinofroilan.tessera.domain.hr.model.ChecklistTaskTemplate
import com.aquinofroilan.tessera.domain.hr.model.ChecklistTemplate
import com.aquinofroilan.tessera.domain.hr.model.ChecklistType
import com.aquinofroilan.tessera.domain.hr.model.Employee
import com.aquinofroilan.tessera.domain.hr.model.EmployeeChecklist
import com.aquinofroilan.tessera.domain.hr.model.EmployeeChecklistTask
import com.aquinofroilan.tessera.domain.hr.model.EmploymentStatus
import com.aquinofroilan.tessera.domain.hr.repository.ChecklistTemplateRepository
import com.aquinofroilan.tessera.domain.hr.repository.EmployeeChecklistRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class ChecklistServiceTest {
    private val checklistTemplateRepository: ChecklistTemplateRepository = mock()
    private val employeeChecklistRepository: EmployeeChecklistRepository = mock()
    private val employeeService: EmployeeService = mock()

    private val service =
        ChecklistService(
            checklistTemplateRepository,
            employeeChecklistRepository,
            employeeService,
        )

    private val orgId = UUID.randomUUID()
    private val employeeId = UUID.randomUUID()
    private val templateId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        val mockEmployee =
            Employee(
                id = employeeId,
                employeeNumber = "E002",
                firstName = "Jane",
                lastName = "Smith",
                hireDate = LocalDate.now(),
                organizationId = orgId,
                status = EmploymentStatus.ACTIVE,
            )
        whenever(employeeService.getEmployee(employeeId, orgId)).thenReturn(mockEmployee)

        val mockTemplate =
            ChecklistTemplate(
                id = templateId,
                organizationId = orgId,
                name = "Standard Onboarding",
                type = ChecklistType.ONBOARDING,
                tasks =
                    mutableListOf(
                        ChecklistTaskTemplate(description = "Setup email", sortOrder = 1),
                        ChecklistTaskTemplate(description = "Assign laptop", sortOrder = 2),
                    ),
            )
        whenever(checklistTemplateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate))

        whenever(checklistTemplateRepository.save(any<ChecklistTemplate>())).thenAnswer { it.arguments[0] }
        whenever(employeeChecklistRepository.save(any<EmployeeChecklist>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `createTemplate saves template with tasks`() {
        val request =
            CreateChecklistTemplateRequest(
                name = "Standard Offboarding",
                type = ChecklistType.OFFBOARDING,
                tasks =
                    listOf(
                        ChecklistTaskTemplateDto(description = "Revoke access"),
                    ),
            )
        val template = service.createTemplate(request, orgId)
        assertThat(template.name).isEqualTo("Standard Offboarding")
        assertThat(template.type).isEqualTo(ChecklistType.OFFBOARDING)
        assertThat(template.tasks).hasSize(1)
        assertThat(template.tasks[0].description).isEqualTo("Revoke access")
    }

    @Test
    fun `spawnChecklistForEmployee generates a checklist based on a template`() {
        val checklist = service.spawnChecklistForEmployee(employeeId, templateId, orgId)
        assertThat(checklist.employeeId).isEqualTo(employeeId)
        assertThat(checklist.type).isEqualTo(ChecklistType.ONBOARDING)
        assertThat(checklist.tasks).hasSize(2)
        assertThat(checklist.tasks[0].description).isEqualTo("Setup email")
        assertThat(checklist.tasks[1].description).isEqualTo("Assign laptop")
        assertThat(checklist.isComplete()).isFalse
    }

    @Test
    fun `completeTask marks task as completed`() {
        val checklistId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val completedBy = UUID.randomUUID()

        val checklist =
            EmployeeChecklist(
                id = checklistId,
                organizationId = orgId,
                employeeId = employeeId,
                type = ChecklistType.ONBOARDING,
                tasks =
                    mutableListOf(
                        EmployeeChecklistTask(id = taskId, checklistId = checklistId, description = "A task"),
                    ),
            )

        whenever(employeeChecklistRepository.findById(checklistId)).thenReturn(Optional.of(checklist))

        val updated = service.completeTask(checklistId, taskId, orgId, completedBy)

        assertThat(updated.tasks[0].isCompleted).isTrue
        assertThat(updated.tasks[0].completedBy).isEqualTo(completedBy)
        assertThat(updated.tasks[0].completedAt).isNotNull
        assertThat(updated.isComplete()).isTrue
    }
}
