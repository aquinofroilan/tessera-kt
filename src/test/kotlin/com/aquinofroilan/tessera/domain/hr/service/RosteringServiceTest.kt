package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateRosterRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateShiftRequest
import com.aquinofroilan.tessera.domain.hr.dto.RosterEntryDto
import com.aquinofroilan.tessera.domain.hr.model.Employee
import com.aquinofroilan.tessera.domain.hr.model.EmploymentStatus
import com.aquinofroilan.tessera.domain.hr.model.Roster
import com.aquinofroilan.tessera.domain.hr.model.Shift
import com.aquinofroilan.tessera.domain.hr.repository.RosterRepository
import com.aquinofroilan.tessera.domain.hr.repository.ShiftRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class RosteringServiceTest {
    private val shiftRepository: ShiftRepository = mock()
    private val rosterRepository: RosterRepository = mock()
    private val employeeService: EmployeeService = mock()

    private val service =
        RosteringService(
            shiftRepository,
            rosterRepository,
            employeeService,
        )

    private val orgId = UUID.randomUUID()
    private val employeeId = UUID.randomUUID()
    private val shiftId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        val mockEmployee =
            Employee(
                id = employeeId,
                employeeNumber = "E003",
                firstName = "Bob",
                lastName = "Builder",
                hireDate = LocalDate.now(),
                organizationId = orgId,
                status = EmploymentStatus.ACTIVE,
            )
        whenever(employeeService.getEmployee(employeeId, orgId)).thenReturn(mockEmployee)

        val mockShift =
            Shift(
                id = shiftId,
                organizationId = orgId,
                name = "Morning",
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(16, 0),
            )
        whenever(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift))

        whenever(shiftRepository.save(any<Shift>())).thenAnswer { it.arguments[0] }
        whenever(rosterRepository.save(any<Roster>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `createShift creates a shift`() {
        val request =
            CreateShiftRequest(
                name = "Evening",
                startTime = LocalTime.of(16, 0),
                endTime = LocalTime.of(0, 0),
            )
        val shift = service.createShift(request, orgId)
        assertThat(shift.name).isEqualTo("Evening")
        assertThat(shift.startTime).isEqualTo(LocalTime.of(16, 0))
    }

    @Test
    fun `createRoster creates a roster with entries`() {
        val request =
            CreateRosterRequest(
                name = "Jan 2026",
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 1, 31),
                entries =
                    listOf(
                        RosterEntryDto(
                            employeeId = employeeId,
                            shiftId = shiftId,
                            shiftDate = LocalDate.of(2026, 1, 1),
                        ),
                    ),
            )
        val roster = service.createRoster(request, orgId)
        assertThat(roster.name).isEqualTo("Jan 2026")
        assertThat(roster.entries).hasSize(1)
        assertThat(roster.entries[0].employeeId).isEqualTo(employeeId)
        assertThat(roster.entries[0].shiftId).isEqualTo(shiftId)
    }
}
