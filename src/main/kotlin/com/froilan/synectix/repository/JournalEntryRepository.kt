package com.froilan.synectix.repository

import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface JournalEntryRepository :
    JpaRepository<JournalEntry, String>,
    JournalEntryAggregations {
    fun findByOrganizationId(organizationId: String): List<JournalEntry>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: JournalEntryStatus,
    ): List<JournalEntry>

    fun findByOrganizationIdAndDateBetween(
        organizationId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusAndDateBetween(
        organizationId: String,
        status: JournalEntryStatus,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusAndDateLessThanEqual(
        organizationId: String,
        status: JournalEntryStatus,
        date: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndDateGreaterThanEqual(
        organizationId: String,
        date: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndDateLessThanEqual(
        organizationId: String,
        date: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusAndDateGreaterThanEqual(
        organizationId: String,
        status: JournalEntryStatus,
        date: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusIn(
        organizationId: String,
        statuses: List<JournalEntryStatus>,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusInAndDateBetween(
        organizationId: String,
        statuses: List<JournalEntryStatus>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusInAndDateLessThanEqual(
        organizationId: String,
        statuses: List<JournalEntryStatus>,
        date: LocalDate,
    ): List<JournalEntry>

    fun countByOrganizationId(organizationId: String): Long

    fun existsByOrganizationIdAndSourceReference(
        organizationId: String,
        sourceReference: String,
    ): Boolean
}
