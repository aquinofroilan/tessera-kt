package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.JournalEntry
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface JournalEntryRepository :
    JpaRepository<JournalEntry, java.util.UUID>,
    JournalEntryAggregations {
    fun findByOrganizationId(organizationId: java.util.UUID): List<JournalEntry>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: JournalEntryStatus,
    ): List<JournalEntry>

    fun findByOrganizationIdAndDateBetween(
        organizationId: java.util.UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusAndDateBetween(
        organizationId: java.util.UUID,
        status: JournalEntryStatus,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusAndDateLessThanEqual(
        organizationId: java.util.UUID,
        status: JournalEntryStatus,
        date: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndDateGreaterThanEqual(
        organizationId: java.util.UUID,
        date: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndDateLessThanEqual(
        organizationId: java.util.UUID,
        date: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusAndDateGreaterThanEqual(
        organizationId: java.util.UUID,
        status: JournalEntryStatus,
        date: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusIn(
        organizationId: java.util.UUID,
        statuses: List<JournalEntryStatus>,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusInAndDateBetween(
        organizationId: java.util.UUID,
        statuses: List<JournalEntryStatus>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<JournalEntry>

    fun findByOrganizationIdAndStatusInAndDateLessThanEqual(
        organizationId: java.util.UUID,
        statuses: List<JournalEntryStatus>,
        date: LocalDate,
    ): List<JournalEntry>

    fun countByOrganizationId(organizationId: java.util.UUID): Long

    fun existsByOrganizationIdAndSourceReference(
        organizationId: java.util.UUID,
        sourceReference: String,
    ): Boolean
}
