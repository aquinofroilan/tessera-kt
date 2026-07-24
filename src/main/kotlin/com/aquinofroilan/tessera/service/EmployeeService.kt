package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateEmployeeRequest
import com.aquinofroilan.tessera.dto.UpdateEmployeeRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Employee
import com.aquinofroilan.tessera.model.EmploymentStatus
import com.aquinofroilan.tessera.repository.EmployeeRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val departmentService: DepartmentService,
) {
    @Transactional
    fun createEmployee(
        request: CreateEmployeeRequest,
        organizationId: java.util.UUID,
    ): Employee {
        val hireDate = request.hireDate ?: throw BusinessRuleException("Hire date is required")
        request.departmentId?.let { requireActiveDepartment(it, organizationId) }
        request.userId?.let { requireUserUnlinked(it, organizationId, excludingEmployeeId = null) }
        return saveWithRetry(organizationId) { number ->
            Employee(
                employeeNumber = number,
                firstName = request.firstName.trim(),
                lastName = request.lastName.trim(),
                email = request.email?.trim(),
                jobTitle = request.jobTitle?.trim(),
                departmentId = request.departmentId,
                userId = request.userId,
                hireDate = hireDate,
                organizationId = organizationId,
            )
        }
    }

    fun getEmployee(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Employee {
        val employee =
            employeeRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Employee not found")
            }
        if (employee.organizationId != organizationId) {
            throw ResourceNotFoundException("Employee not found")
        }
        return employee
    }

    fun listEmployees(
        organizationId: java.util.UUID,
        status: EmploymentStatus? = null,
        departmentId: java.util.UUID? = null,
    ): List<Employee> =
        when {
            status != null -> employeeRepository.findByOrganizationIdAndStatus(organizationId, status)
            departmentId != null -> employeeRepository.findByOrganizationIdAndDepartmentId(organizationId, departmentId)
            else -> employeeRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateEmployee(
        id: java.util.UUID,
        request: UpdateEmployeeRequest,
        organizationId: java.util.UUID,
    ): Employee {
        val employee = getEmployee(id, organizationId)
        request.userId?.let { requireUserUnlinked(it, organizationId, excludingEmployeeId = employee.id) }
        employee.apply {
            firstName = request.firstName?.trim() ?: employee.firstName
            lastName = request.lastName?.trim() ?: employee.lastName
            email = request.email?.trim() ?: employee.email
            jobTitle = request.jobTitle?.trim() ?: employee.jobTitle
            userId = request.userId ?: employee.userId
        }
        return employeeRepository.save(employee)
    }

    /** Resolves the employee record linked to the given login user, for self-service. */
    fun getEmployeeByUser(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
    ): Employee =
        employeeRepository.findByOrganizationIdAndUserId(organizationId, userId).orElseThrow {
            ResourceNotFoundException("No employee record is linked to your account")
        }

    @Transactional
    fun assignDepartment(
        id: java.util.UUID,
        departmentId: java.util.UUID?,
        organizationId: java.util.UUID,
    ): Employee {
        val employee = getEmployee(id, organizationId)
        if (employee.status == EmploymentStatus.TERMINATED) {
            throw BusinessRuleException("Cannot reassign a terminated employee")
        }
        departmentId?.let { requireActiveDepartment(it, organizationId) }
        employee.departmentId = departmentId
        return employeeRepository.save(employee)
    }

    @Transactional
    fun placeOnLeave(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Employee {
        val employee = getEmployee(id, organizationId)
        if (employee.status != EmploymentStatus.ACTIVE) {
            throw BusinessRuleException("Only active employees can be placed on leave")
        }
        employee.status = EmploymentStatus.ON_LEAVE
        return employeeRepository.save(employee)
    }

    @Transactional
    fun returnFromLeave(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Employee {
        val employee = getEmployee(id, organizationId)
        if (employee.status != EmploymentStatus.ON_LEAVE) {
            throw BusinessRuleException("Only employees on leave can return")
        }
        employee.status = EmploymentStatus.ACTIVE
        return employeeRepository.save(employee)
    }

    @Transactional
    fun terminate(
        id: java.util.UUID,
        terminationDate: LocalDate,
        organizationId: java.util.UUID,
    ): Employee {
        val employee = getEmployee(id, organizationId)
        if (employee.status == EmploymentStatus.TERMINATED) {
            throw BusinessRuleException("Employee is already terminated")
        }
        if (terminationDate.isBefore(employee.hireDate)) {
            throw BusinessRuleException("Termination date cannot be before the hire date")
        }
        employee.status = EmploymentStatus.TERMINATED
        employee.terminationDate = terminationDate
        return employeeRepository.save(employee)
    }

    private fun requireUserUnlinked(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
        excludingEmployeeId: java.util.UUID?,
    ) {
        val existing = employeeRepository.findByOrganizationIdAndUserId(organizationId, userId)
        if (existing.isPresent && existing.get().id != excludingEmployeeId) {
            throw BusinessRuleException("That user is already linked to another employee")
        }
    }

    private fun requireActiveDepartment(
        departmentId: java.util.UUID,
        organizationId: java.util.UUID,
    ) {
        val department = departmentService.getDepartment(departmentId, organizationId)
        if (!department.isActive) {
            throw BusinessRuleException("Department '${department.code}' is inactive")
        }
    }

    private fun saveWithRetry(
        organizationId: java.util.UUID,
        maxRetries: Int = 3,
        build: (String) -> Employee,
    ): Employee {
        repeat(maxRetries) { attempt ->
            val count = employeeRepository.countByOrganizationId(organizationId)
            val number = "EMP-${(count + 1).toString().padStart(4, '0')}"
            try {
                return employeeRepository.save(build(number))
            } catch (e: DuplicateKeyException) {
                if (attempt == maxRetries - 1) {
                    throw IllegalStateException("Failed to generate unique employee number: $number", e)
                }
            }
        }
        throw IllegalStateException("Failed to generate unique employee number")
    }
}
