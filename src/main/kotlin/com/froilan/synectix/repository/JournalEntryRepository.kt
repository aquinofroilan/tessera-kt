package com.froilan.synectix.repository

import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryStatus
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface JournalEntryRepository : MongoRepository<JournalEntry, String> {
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

    fun countByOrganizationId(organizationId: String): Long
}
