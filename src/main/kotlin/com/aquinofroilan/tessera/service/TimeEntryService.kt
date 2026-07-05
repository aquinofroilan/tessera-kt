package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateTimeEntryRequest
import com.aquinofroilan.tessera.dto.UpdateTimeEntryRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.TimeEntry
import com.aquinofroilan.tessera.model.TimeEntryStatus
import com.aquinofroilan.tessera.repository.TimeEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class TimeEntryService(
    private val timeEntryRepository: TimeEntryRepository,
    private val projectService: ProjectService,
    private val projectTaskService: ProjectTaskService,
    private val employeeService: EmployeeService,
) {
    @Transactional
    fun createTimeEntry(
        request: CreateTimeEntryRequest,
        organizationId: String,
    ): TimeEntry {
        val hours = request.hours ?: throw BusinessRuleException("Hours are required")
        if (hours.signum() <= 0) {
            throw BusinessRuleException("Hours must be positive")
        }
        val entryDate = request.entryDate ?: throw BusinessRuleException("Entry date is required")
        if (request.rate != null && request.rate.signum() < 0) {
            throw BusinessRuleException("Rate must not be negative")
        }
        projectService.getProject(request.projectId, organizationId)
        request.taskId?.let { projectTaskService.getTask(request.projectId, it, organizationId) }
        employeeService.getEmployee(request.employeeId, organizationId)

        return timeEntryRepository.save(
            TimeEntry(
                employeeId = request.employeeId,
                projectId = request.projectId,
                taskId = request.taskId,
                entryDate = entryDate,
                hours = hours,
                billable = request.billable,
                rate = request.rate,
                notes = request.notes,
                organizationId = organizationId,
            ),
        )
    }

    fun getTimeEntry(
        id: String,
        organizationId: String,
    ): TimeEntry {
        val entry =
            timeEntryRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Time entry not found")
            }
        if (entry.organizationId != organizationId) {
            throw ResourceNotFoundException("Time entry not found")
        }
        return entry
    }

    fun listTimeEntries(
        organizationId: String,
        employeeId: String? = null,
        projectId: String? = null,
        status: TimeEntryStatus? = null,
    ): List<TimeEntry> =
        when {
            projectId != null && status != null ->
                timeEntryRepository.findByOrganizationIdAndProjectIdAndStatus(organizationId, projectId, status)
            employeeId != null -> timeEntryRepository.findByOrganizationIdAndEmployeeId(organizationId, employeeId)
            projectId != null -> timeEntryRepository.findByOrganizationIdAndProjectId(organizationId, projectId)
            status != null -> timeEntryRepository.findByOrganizationIdAndStatus(organizationId, status)
            else -> timeEntryRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateTimeEntry(
        id: String,
        request: UpdateTimeEntryRequest,
        organizationId: String,
    ): TimeEntry {
        val entry = getTimeEntry(id, organizationId)
        if (entry.status != TimeEntryStatus.DRAFT) {
            throw BusinessRuleException("Only draft time entries can be edited")
        }
        request.hours?.let { if (it.signum() <= 0) throw BusinessRuleException("Hours must be positive") }
        request.taskId?.let { projectTaskService.getTask(entry.projectId, it, organizationId) }
        entry.apply {
            taskId = request.taskId ?: entry.taskId
            entryDate = request.entryDate ?: entry.entryDate
            hours = request.hours ?: entry.hours
            billable = request.billable ?: entry.billable
            rate = request.rate ?: entry.rate
            notes = request.notes ?: entry.notes
        }
        return timeEntryRepository.save(entry)
    }

    @Transactional
    fun submitTimeEntry(
        id: String,
        organizationId: String,
    ): TimeEntry {
        val entry = getTimeEntry(id, organizationId)
        if (entry.status != TimeEntryStatus.DRAFT) {
            throw BusinessRuleException("Only draft time entries can be submitted")
        }
        entry.status = TimeEntryStatus.SUBMITTED
        return timeEntryRepository.save(entry)
    }

    @Transactional
    fun approveTimeEntry(
        id: String,
        organizationId: String,
        approvedBy: String,
    ): TimeEntry {
        val entry = getTimeEntry(id, organizationId)
        if (entry.status != TimeEntryStatus.SUBMITTED) {
            throw BusinessRuleException("Only submitted time entries can be approved")
        }
        entry.status = TimeEntryStatus.APPROVED
        entry.approvedBy = approvedBy
        entry.approvedAt = LocalDateTime.now(ZoneOffset.UTC)
        return timeEntryRepository.save(entry)
    }

    @Transactional
    fun rejectTimeEntry(
        id: String,
        organizationId: String,
        decidedBy: String,
    ): TimeEntry {
        val entry = getTimeEntry(id, organizationId)
        if (entry.status != TimeEntryStatus.SUBMITTED) {
            throw BusinessRuleException("Only submitted time entries can be rejected")
        }
        entry.status = TimeEntryStatus.REJECTED
        entry.approvedBy = decidedBy
        entry.approvedAt = LocalDateTime.now(ZoneOffset.UTC)
        return timeEntryRepository.save(entry)
    }
}
