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

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
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
        id: java.util.UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
        org: java.util.UUID = orgId,
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
        whenever(departmentService.getDepartment(java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"), orgId))
            .thenReturn(
                Department(
                    id = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                    code = "ENG",
                    name = "Engineering",
                    organizationId = orgId,
                    isActive = false,
                ),
            )

        assertThatThrownBy {
            service.createEmployee(
                CreateEmployeeRequest(
                    firstName = "Ada",
                    lastName = "Lovelace",
                    departmentId = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                    hireDate = hireDate,
                ),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `leave and return follow the lifecycle`() {
        whenever(repository.findById(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"))).thenReturn(Optional.of(employee()))
        assertThat(
            service.placeOnLeave(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"), orgId).status,
        ).isEqualTo(EmploymentStatus.ON_LEAVE)

        whenever(
            repository.findById(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4")),
        ).thenReturn(Optional.of(employee(status = EmploymentStatus.ON_LEAVE)))
        assertThat(
            service.returnFromLeave(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"), orgId).status,
        ).isEqualTo(EmploymentStatus.ACTIVE)
    }

    @Test
    fun `cannot place a terminated employee on leave`() {
        whenever(
            repository.findById(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4")),
        ).thenReturn(Optional.of(employee(status = EmploymentStatus.TERMINATED)))
        assertThatThrownBy {
            service.placeOnLeave(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `terminate sets status and date and rejects a date before hire`() {
        whenever(repository.findById(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"))).thenReturn(Optional.of(employee()))
        val terminated =
            service.terminate(
                java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"),
                LocalDate.of(2026, 6, 30),
                orgId,
            )
        assertThat(terminated.status).isEqualTo(EmploymentStatus.TERMINATED)
        assertThat(terminated.terminationDate).isEqualTo(LocalDate.of(2026, 6, 30))

        whenever(repository.findById(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"))).thenReturn(Optional.of(employee()))
        assertThatThrownBy {
            service.terminate(
                java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"),
                LocalDate.of(2025, 1, 1),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `assign department rejects a terminated employee`() {
        whenever(
            repository.findById(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4")),
        ).thenReturn(Optional.of(employee(status = EmploymentStatus.TERMINATED)))
        assertThatThrownBy {
            service.assignDepartment(
                java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"),
                java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create links a user`() {
        whenever(
            repository.findByOrganizationIdAndUserId(orgId, java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb")),
        ).thenReturn(Optional.empty())
        val emp =
            service.createEmployee(
                CreateEmployeeRequest(
                    firstName = "Ada",
                    lastName = "Lovelace",
                    hireDate = hireDate,
                    userId = java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"),
                ),
                orgId,
            )
        assertThat(emp.userId).isEqualTo(java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"))
    }

    @Test
    fun `create rejects linking a user already linked to another employee`() {
        whenever(repository.findByOrganizationIdAndUserId(orgId, java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb")))
            .thenReturn(Optional.of(employee(id = java.util.UUID.fromString("f022a845-ae01-3e07-ae04-7fc0ffb096a8"))))
        assertThatThrownBy {
            service.createEmployee(
                CreateEmployeeRequest(
                    firstName = "Ada",
                    lastName = "Lovelace",
                    hireDate = hireDate,
                    userId = java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"),
                ),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `getEmployeeByUser resolves the linked employee or throws`() {
        whenever(
            repository.findByOrganizationIdAndUserId(orgId, java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb")),
        ).thenReturn(Optional.of(employee()))
        assertThat(
            service.getEmployeeByUser(java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"), orgId).id,
        ).isEqualTo(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))

        whenever(
            repository.findByOrganizationIdAndUserId(orgId, java.util.UUID.fromString("731b3177-ae56-32b2-9b3b-f13f4cde1ee2")),
        ).thenReturn(Optional.empty())
        assertThatThrownBy { service.getEmployeeByUser(java.util.UUID.fromString("731b3177-ae56-32b2-9b3b-f13f4cde1ee2"), orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }
}
