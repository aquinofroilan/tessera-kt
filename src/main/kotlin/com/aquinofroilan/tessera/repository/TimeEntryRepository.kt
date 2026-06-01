package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.TimeEntry
import com.aquinofroilan.tessera.model.TimeEntryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TimeEntryRepository : JpaRepository<TimeEntry, String> {
    fun findByOrganizationId(organizationId: String): List<TimeEntry>

    fun findByOrganizationIdAndEmployeeId(
        organizationId: String,
        employeeId: String,
    ): List<TimeEntry>

    fun findByOrganizationIdAndProjectId(
        organizationId: String,
        projectId: String,
    ): List<TimeEntry>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: TimeEntryStatus,
    ): List<TimeEntry>

    fun findByOrganizationIdAndProjectIdAndStatus(
        organizationId: String,
        projectId: String,
        status: TimeEntryStatus,
    ): List<TimeEntry>
}
