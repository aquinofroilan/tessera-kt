package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateEmployeeRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Department
import com.aquinofroilan.tessera.model.Employee
import com.aquinofroilan.tessera.model.EmploymentStatus
import com.aquinofroilan.tessera.repository.EmployeeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

class EmployeeServiceTest {
    private lateinit var repository: EmployeeRepository
    private lateinit var departmentService: DepartmentService
    private lateinit var service: EmployeeService

    private val orgId = "org-1"
    private val hireDate = LocalDate.of(2026, 1, 6)

    @BeforeEach
    fun setup() {
        repository = mock(EmployeeRepository::class.java)
        departmentService = mock(DepartmentService::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<Employee>())).thenAnswer { it.arguments[0] }
        service = EmployeeService(repository, departmentService)
    }

    private fun employee(
        status: EmploymentStatus = EmploymentStatus.ACTIVE,
        id: String = "e1",
        org: String = orgId,
    ) = Employee(
        id = id,
        employeeNumber = "EMP-0001",
        firstName = "Ada",
        lastName = "Lovelace",
        hireDate = hireDate,
        status = status,
        organizationId = org,
    )

    @Test
    fun `create assigns a number and starts ACTIVE`() {
        val emp = service.createEmployee(CreateEmployeeRequest(firstName = "Ada", lastName = "Lovelace", hireDate = hireDate), orgId)

        assertThat(emp.employeeNumber).isEqualTo("EMP-0001")
        assertThat(emp.status).isEqualTo(EmploymentStatus.ACTIVE)
    }

    @Test
    fun `create validates an inactive department`() {
        whenever(departmentService.getDepartment("d1", orgId))
            .thenReturn(Department(id = "d1", code = "ENG", name = "Engineering", organizationId = orgId, isActive = false))

        assertThatThrownBy {
            service.createEmployee(
                CreateEmployeeRequest(firstName = "Ada", lastName = "Lovelace", departmentId = "d1", hireDate = hireDate),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `leave and return follow the lifecycle`() {
        whenever(repository.findById("e1")).thenReturn(Optional.of(employee()))
        assertThat(service.placeOnLeave("e1", orgId).status).isEqualTo(EmploymentStatus.ON_LEAVE)

        whenever(repository.findById("e1")).thenReturn(Optional.of(employee(status = EmploymentStatus.ON_LEAVE)))
        assertThat(service.returnFromLeave("e1", orgId).status).isEqualTo(EmploymentStatus.ACTIVE)
    }

    @Test
    fun `cannot place a terminated employee on leave`() {
        whenever(repository.findById("e1")).thenReturn(Optional.of(employee(status = EmploymentStatus.TERMINATED)))
        assertThatThrownBy { service.placeOnLeave("e1", orgId) }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `terminate sets status and date and rejects a date before hire`() {
        whenever(repository.findById("e1")).thenReturn(Optional.of(employee()))
        val terminated = service.terminate("e1", LocalDate.of(2026, 6, 30), orgId)
        assertThat(terminated.status).isEqualTo(EmploymentStatus.TERMINATED)
        assertThat(terminated.terminationDate).isEqualTo(LocalDate.of(2026, 6, 30))

        whenever(repository.findById("e1")).thenReturn(Optional.of(employee()))
        assertThatThrownBy { service.terminate("e1", LocalDate.of(2025, 1, 1), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `assign department rejects a terminated employee`() {
        whenever(repository.findById("e1")).thenReturn(Optional.of(employee(status = EmploymentStatus.TERMINATED)))
        assertThatThrownBy { service.assignDepartment("e1", "d1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create links a user`() {
        whenever(repository.findByOrganizationIdAndUserId(orgId, "u1")).thenReturn(Optional.empty())
        val emp =
            service.createEmployee(
                CreateEmployeeRequest(firstName = "Ada", lastName = "Lovelace", hireDate = hireDate, userId = "u1"),
                orgId,
            )
        assertThat(emp.userId).isEqualTo("u1")
    }

    @Test
    fun `create rejects linking a user already linked to another employee`() {
        whenever(repository.findByOrganizationIdAndUserId(orgId, "u1"))
            .thenReturn(Optional.of(employee(id = "other")))
        assertThatThrownBy {
            service.createEmployee(
                CreateEmployeeRequest(firstName = "Ada", lastName = "Lovelace", hireDate = hireDate, userId = "u1"),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `getEmployeeByUser resolves the linked employee or throws`() {
        whenever(repository.findByOrganizationIdAndUserId(orgId, "u1")).thenReturn(Optional.of(employee()))
        assertThat(service.getEmployeeByUser("u1", orgId).id).isEqualTo("e1")

        whenever(repository.findByOrganizationIdAndUserId(orgId, "u2")).thenReturn(Optional.empty())
        assertThatThrownBy { service.getEmployeeByUser("u2", orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }
}
