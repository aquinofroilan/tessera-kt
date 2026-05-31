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
        organizationId: String,
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
        id: String,
        organizationId: String,
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
        organizationId: String,
        status: EmploymentStatus? = null,
        departmentId: String? = null,
    ): List<Employee> =
        when {
            status != null -> employeeRepository.findByOrganizationIdAndStatus(organizationId, status)
            departmentId != null -> employeeRepository.findByOrganizationIdAndDepartmentId(organizationId, departmentId)
            else -> employeeRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateEmployee(
        id: String,
        request: UpdateEmployeeRequest,
        organizationId: String,
    ): Employee {
        val employee = getEmployee(id, organizationId)
        request.userId?.let { requireUserUnlinked(it, organizationId, excludingEmployeeId = employee.id) }
        return employeeRepository.save(
            employee.copy(
                firstName = request.firstName?.trim() ?: employee.firstName,
                lastName = request.lastName?.trim() ?: employee.lastName,
                email = request.email?.trim() ?: employee.email,
                jobTitle = request.jobTitle?.trim() ?: employee.jobTitle,
                userId = request.userId ?: employee.userId,
            ),
        )
    }

    /** Resolves the employee record linked to the given login user, for self-service. */
    fun getEmployeeByUser(
        userId: String,
        organizationId: String,
    ): Employee =
        employeeRepository.findByOrganizationIdAndUserId(organizationId, userId).orElseThrow {
            ResourceNotFoundException("No employee record is linked to your account")
        }

    @Transactional
    fun assignDepartment(
        id: String,
        departmentId: String?,
        organizationId: String,
    ): Employee {
        val employee = getEmployee(id, organizationId)
        if (employee.status == EmploymentStatus.TERMINATED) {
            throw BusinessRuleException("Cannot reassign a terminated employee")
        }
        departmentId?.let { requireActiveDepartment(it, organizationId) }
        return employeeRepository.save(employee.copy(departmentId = departmentId))
    }

    @Transactional
    fun placeOnLeave(
        id: String,
        organizationId: String,
    ): Employee {
        val employee = getEmployee(id, organizationId)
        if (employee.status != EmploymentStatus.ACTIVE) {
            throw BusinessRuleException("Only active employees can be placed on leave")
        }
        return employeeRepository.save(employee.copy(status = EmploymentStatus.ON_LEAVE))
    }

    @Transactional
    fun returnFromLeave(
        id: String,
        organizationId: String,
    ): Employee {
        val employee = getEmployee(id, organizationId)
        if (employee.status != EmploymentStatus.ON_LEAVE) {
            throw BusinessRuleException("Only employees on leave can return")
        }
        return employeeRepository.save(employee.copy(status = EmploymentStatus.ACTIVE))
    }

    @Transactional
    fun terminate(
        id: String,
        terminationDate: LocalDate,
        organizationId: String,
    ): Employee {
        val employee = getEmployee(id, organizationId)
        if (employee.status == EmploymentStatus.TERMINATED) {
            throw BusinessRuleException("Employee is already terminated")
        }
        if (terminationDate.isBefore(employee.hireDate)) {
            throw BusinessRuleException("Termination date cannot be before the hire date")
        }
        return employeeRepository.save(
            employee.copy(status = EmploymentStatus.TERMINATED, terminationDate = terminationDate),
        )
    }

    private fun requireUserUnlinked(
        userId: String,
        organizationId: String,
        excludingEmployeeId: String?,
    ) {
        val existing = employeeRepository.findByOrganizationIdAndUserId(organizationId, userId)
        if (existing.isPresent && existing.get().id != excludingEmployeeId) {
            throw BusinessRuleException("That user is already linked to another employee")
        }
    }

    private fun requireActiveDepartment(
        departmentId: String,
        organizationId: String,
    ) {
        val department = departmentService.getDepartment(departmentId, organizationId)
        if (!department.isActive) {
            throw BusinessRuleException("Department '${department.code}' is inactive")
        }
    }

    private fun saveWithRetry(
        organizationId: String,
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
