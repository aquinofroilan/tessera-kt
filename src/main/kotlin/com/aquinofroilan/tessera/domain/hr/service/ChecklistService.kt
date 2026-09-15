package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateChecklistTemplateRequest
import com.aquinofroilan.tessera.domain.hr.model.ChecklistTaskTemplate
import com.aquinofroilan.tessera.domain.hr.model.ChecklistTemplate
import com.aquinofroilan.tessera.domain.hr.model.EmployeeChecklist
import com.aquinofroilan.tessera.domain.hr.model.EmployeeChecklistTask
import com.aquinofroilan.tessera.domain.hr.repository.ChecklistTemplateRepository
import com.aquinofroilan.tessera.domain.hr.repository.EmployeeChecklistRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ChecklistService(
    private val checklistTemplateRepository: ChecklistTemplateRepository,
    private val employeeChecklistRepository: EmployeeChecklistRepository,
    private val employeeService: EmployeeService,
) {
    @Transactional
    fun createTemplate(
        request: CreateChecklistTemplateRequest,
        organizationId: UUID,
    ): ChecklistTemplate {
        val template =
            ChecklistTemplate(
                organizationId = organizationId,
                name = request.name ?: throw BusinessRuleException("Name is required"),
                type = request.type ?: throw BusinessRuleException("Type is required"),
            )
        request.tasks.forEach { taskDto ->
            template.tasks.add(
                ChecklistTaskTemplate(
                    description = taskDto.description ?: throw BusinessRuleException("Task description is required"),
                    isRequired = taskDto.isRequired,
                    sortOrder = taskDto.sortOrder,
                ),
            )
        }
        return checklistTemplateRepository.save(template)
    }

    @Transactional
    fun spawnChecklistForEmployee(
        employeeId: UUID,
        templateId: UUID,
        organizationId: UUID,
    ): EmployeeChecklist {
        employeeService.getEmployee(employeeId, organizationId)

        val template =
            checklistTemplateRepository.findById(templateId).orElseThrow {
                ResourceNotFoundException("Checklist template not found")
            }
        if (template.organizationId != organizationId) {
            throw ResourceNotFoundException("Checklist template not found")
        }

        val employeeChecklist =
            EmployeeChecklist(
                organizationId = organizationId,
                employeeId = employeeId,
                type = template.type,
            )

        template.tasks.forEach { taskTemplate ->
            employeeChecklist.tasks.add(
                EmployeeChecklistTask(
                    description = taskTemplate.description,
                    isRequired = taskTemplate.isRequired,
                    sortOrder = taskTemplate.sortOrder,
                ),
            )
        }

        return employeeChecklistRepository.save(employeeChecklist)
    }

    @Transactional
    fun completeTask(
        checklistId: UUID,
        taskId: UUID,
        organizationId: UUID,
        completedByUserId: UUID,
    ): EmployeeChecklist {
        val checklist =
            employeeChecklistRepository.findById(checklistId).orElseThrow {
                ResourceNotFoundException("Checklist not found")
            }
        if (checklist.organizationId != organizationId) {
            throw ResourceNotFoundException("Checklist not found")
        }

        val task =
            checklist.tasks.find { it.id == taskId }
                ?: throw ResourceNotFoundException("Task not found in checklist")

        if (!task.isCompleted) {
            task.isCompleted = true
            task.completedAt = LocalDateTime.now()
            task.completedBy = completedByUserId
            employeeChecklistRepository.save(checklist)
        }

        return checklist
    }

    fun getEmployeeChecklists(
        employeeId: UUID,
        organizationId: UUID,
    ): List<EmployeeChecklist> = employeeChecklistRepository.findByOrganizationIdAndEmployeeId(organizationId, employeeId)
}
