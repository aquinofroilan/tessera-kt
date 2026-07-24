package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.TimeEntry
import com.aquinofroilan.tessera.model.TimeEntryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TimeEntryRepository : JpaRepository<TimeEntry, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<TimeEntry>

    fun findByOrganizationIdAndEmployeeId(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<TimeEntry>

    fun findByOrganizationIdAndProjectId(
        organizationId: java.util.UUID,
        projectId: java.util.UUID,
    ): List<TimeEntry>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: TimeEntryStatus,
    ): List<TimeEntry>

    fun findByOrganizationIdAndProjectIdAndStatus(
        organizationId: java.util.UUID,
        projectId: java.util.UUID,
        status: TimeEntryStatus,
    ): List<TimeEntry>
}
