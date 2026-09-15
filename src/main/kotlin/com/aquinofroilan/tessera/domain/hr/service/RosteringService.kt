package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateRosterRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateShiftRequest
import com.aquinofroilan.tessera.domain.hr.model.Roster
import com.aquinofroilan.tessera.domain.hr.model.RosterEntry
import com.aquinofroilan.tessera.domain.hr.model.Shift
import com.aquinofroilan.tessera.domain.hr.repository.RosterRepository
import com.aquinofroilan.tessera.domain.hr.repository.ShiftRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RosteringService(
    private val shiftRepository: ShiftRepository,
    private val rosterRepository: RosterRepository,
    private val employeeService: EmployeeService,
) {
    @Transactional
    fun createShift(
        request: CreateShiftRequest,
        organizationId: UUID,
    ): Shift {
        val shift =
            Shift(
                organizationId = organizationId,
                name = request.name ?: throw BusinessRuleException("Name is required"),
                startTime = request.startTime ?: throw BusinessRuleException("Start time is required"),
                endTime = request.endTime ?: throw BusinessRuleException("End time is required"),
            )
        return shiftRepository.save(shift)
    }

    fun getShift(
        shiftId: UUID,
        organizationId: UUID,
    ): Shift {
        val shift =
            shiftRepository.findById(shiftId).orElseThrow {
                ResourceNotFoundException("Shift not found")
            }
        if (shift.organizationId != organizationId) {
            throw ResourceNotFoundException("Shift not found")
        }
        return shift
    }

    @Transactional
    fun createRoster(
        request: CreateRosterRequest,
        organizationId: UUID,
    ): Roster {
        val roster =
            Roster(
                organizationId = organizationId,
                name = request.name ?: throw BusinessRuleException("Name is required"),
                startDate = request.startDate ?: throw BusinessRuleException("Start date is required"),
                endDate = request.endDate ?: throw BusinessRuleException("End date is required"),
            )
        request.entries.forEach { entryDto ->
            val empId = entryDto.employeeId ?: throw BusinessRuleException("Employee ID is required")
            val shiftId = entryDto.shiftId ?: throw BusinessRuleException("Shift ID is required")
            val date = entryDto.shiftDate ?: throw BusinessRuleException("Shift date is required")

            employeeService.getEmployee(empId, organizationId)
            getShift(shiftId, organizationId)

            roster.entries.add(
                RosterEntry(
                    employeeId = empId,
                    shiftId = shiftId,
                    shiftDate = date,
                ),
            )
        }
        return rosterRepository.save(roster)
    }
}
