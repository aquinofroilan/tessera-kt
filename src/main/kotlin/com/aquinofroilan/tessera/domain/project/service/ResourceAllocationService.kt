package com.aquinofroilan.tessera.domain.project.service

import com.aquinofroilan.tessera.domain.hr.service.EmployeeService
import com.aquinofroilan.tessera.domain.project.dto.CreateResourceAllocationRequest
import com.aquinofroilan.tessera.domain.project.dto.ResourceUtilizationReport
import com.aquinofroilan.tessera.domain.project.dto.UpdateResourceAllocationRequest
import com.aquinofroilan.tessera.domain.project.model.ProjectResourceAllocation
import com.aquinofroilan.tessera.domain.project.repository.ProjectResourceAllocationRepository
import com.aquinofroilan.tessera.domain.project.repository.TimeEntryRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ResourceAllocationService(
    private val allocationRepository: ProjectResourceAllocationRepository,
    private val timeEntryRepository: TimeEntryRepository,
    private val projectService: ProjectService,
    private val employeeService: EmployeeService,
) {
    @Transactional
    fun createAllocation(
        projectId: UUID,
        request: CreateResourceAllocationRequest,
        organizationId: UUID,
    ): ProjectResourceAllocation {
        projectService.getProject(projectId, organizationId)
        val empId = request.employeeId ?: throw BusinessRuleException("Employee ID is required")
        employeeService.getEmployee(empId, organizationId)

        val startDate = request.startDate ?: throw BusinessRuleException("Start date is required")
        val endDate = request.endDate ?: throw BusinessRuleException("End date is required")
        val hours = request.allocatedHours ?: throw BusinessRuleException("Allocated hours is required")

        if (endDate.isBefore(startDate)) {
            throw BusinessRuleException("End date cannot be before start date")
        }
        if (hours < BigDecimal.ZERO) {
            throw BusinessRuleException("Allocated hours cannot be negative")
        }

        return allocationRepository.save(
            ProjectResourceAllocation(
                organizationId = organizationId,
                projectId = projectId,
                employeeId = empId,
                startDate = startDate,
                endDate = endDate,
                allocatedHours = hours,
            ),
        )
    }

    fun getAllocationsForProject(
        projectId: UUID,
        organizationId: UUID,
    ): List<ProjectResourceAllocation> {
        projectService.getProject(projectId, organizationId)
        return allocationRepository.findByOrganizationIdAndProjectId(organizationId, projectId)
    }

    fun getAllocation(
        projectId: UUID,
        allocationId: UUID,
        organizationId: UUID,
    ): ProjectResourceAllocation {
        val allocation =
            allocationRepository.findById(allocationId).orElseThrow {
                ResourceNotFoundException("Allocation not found")
            }
        if (allocation.organizationId != organizationId || allocation.projectId != projectId) {
            throw ResourceNotFoundException("Allocation not found")
        }
        return allocation
    }

    @Transactional
    fun updateAllocation(
        projectId: UUID,
        allocationId: UUID,
        request: UpdateResourceAllocationRequest,
        organizationId: UUID,
    ): ProjectResourceAllocation {
        val allocation = getAllocation(projectId, allocationId, organizationId)
        allocation.apply {
            startDate = request.startDate ?: startDate
            endDate = request.endDate ?: endDate
            allocatedHours = request.allocatedHours ?: allocatedHours
        }
        if (allocation.endDate.isBefore(allocation.startDate)) {
            throw BusinessRuleException("End date cannot be before start date")
        }
        if (allocation.allocatedHours < BigDecimal.ZERO) {
            throw BusinessRuleException("Allocated hours cannot be negative")
        }
        return allocationRepository.save(allocation)
    }

    @Transactional
    fun deleteAllocation(
        projectId: UUID,
        allocationId: UUID,
        organizationId: UUID,
    ) {
        val allocation = getAllocation(projectId, allocationId, organizationId)
        allocationRepository.delete(allocation)
    }

    fun generateUtilizationReport(
        employeeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        organizationId: UUID,
    ): ResourceUtilizationReport {
        if (endDate.isBefore(startDate)) {
            throw BusinessRuleException("End date cannot be before start date")
        }
        employeeService.getEmployee(employeeId, organizationId)

        // 1. Calculate allocated hours in this date range
        val allocations = allocationRepository.findByOrganizationIdAndEmployeeId(organizationId, employeeId)
        var totalAllocated = BigDecimal.ZERO

        for (alloc in allocations) {
            // Find intersection of [alloc.startDate, alloc.endDate] and [startDate, endDate]
            val maxStart = if (alloc.startDate.isAfter(startDate)) alloc.startDate else startDate
            val minEnd = if (alloc.endDate.isBefore(endDate)) alloc.endDate else endDate

            if (!maxStart.isAfter(minEnd)) {
                // There is overlap
                val overlapDays = ChronoUnit.DAYS.between(maxStart, minEnd) + 1
                val totalDays = ChronoUnit.DAYS.between(alloc.startDate, alloc.endDate) + 1

                if (totalDays > 0) {
                    val ratio = BigDecimal.valueOf(overlapDays).divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP)
                    totalAllocated = totalAllocated.add(alloc.allocatedHours.multiply(ratio))
                }
            }
        }

        // 2. Calculate actual hours
        val timeEntries =
            timeEntryRepository.findByOrganizationIdAndEmployeeIdAndEntryDateBetween(
                organizationId,
                employeeId,
                startDate,
                endDate,
            )
        val totalActual = timeEntries.map { it.hours }.fold(BigDecimal.ZERO, BigDecimal::add)

        // 3. Compute utilization
        val utilization =
            if (totalAllocated > BigDecimal.ZERO) {
                totalActual.multiply(BigDecimal("100")).divide(totalAllocated, 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

        return ResourceUtilizationReport(
            employeeId = employeeId,
            startDate = startDate,
            endDate = endDate,
            allocatedHours = totalAllocated.setScale(2, RoundingMode.HALF_UP),
            actualHours = totalActual.setScale(2, RoundingMode.HALF_UP),
            utilizationPercentage = utilization,
        )
    }
}
