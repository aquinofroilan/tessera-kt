package com.loom.synectix.service

import com.loom.synectix.dto.CreateDepartmentRequest
import com.loom.synectix.dto.UpdateDepartmentRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.exception.ResourceNotFoundException
import com.loom.synectix.model.Department
import com.loom.synectix.repository.DepartmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DepartmentService(
    private val departmentRepository: DepartmentRepository,
) {
    @Transactional
    fun createDepartment(
        request: CreateDepartmentRequest,
        organizationId: String,
    ): Department {
        val code = request.code.trim()
        if (departmentRepository.findByOrganizationIdAndCode(organizationId, code).isPresent) {
            throw BusinessRuleException("Department code '$code' already exists")
        }
        return departmentRepository.save(
            Department(
                code = code,
                name = request.name.trim(),
                description = request.description,
                organizationId = organizationId,
            ),
        )
    }

    fun getDepartment(
        id: String,
        organizationId: String,
    ): Department {
        val department =
            departmentRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Department not found")
            }
        if (department.organizationId != organizationId) {
            throw ResourceNotFoundException("Department not found")
        }
        return department
    }

    fun listDepartments(
        organizationId: String,
        activeOnly: Boolean = false,
    ): List<Department> =
        if (activeOnly) {
            departmentRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            departmentRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateDepartment(
        id: String,
        request: UpdateDepartmentRequest,
        organizationId: String,
    ): Department {
        val department = getDepartment(id, organizationId)
        return departmentRepository.save(
            department.copy(
                name = request.name?.trim() ?: department.name,
                description = request.description ?: department.description,
            ),
        )
    }

    @Transactional
    fun deactivateDepartment(
        id: String,
        organizationId: String,
    ): Department {
        val department = getDepartment(id, organizationId)
        if (!department.isActive) {
            throw BusinessRuleException("Department is already inactive")
        }
        return departmentRepository.save(department.copy(isActive = false))
    }
}
