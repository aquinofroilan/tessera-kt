package com.froilan.synectix.service

import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.repository.JournalEntryRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component

@Component
class JournalEntryNumberGenerator(
    private val journalEntryRepository: JournalEntryRepository,
) {
    fun saveWithRetry(
        organizationId: String,
        maxRetries: Int = 3,
        buildEntry: (String) -> JournalEntry,
    ): JournalEntry {
        repeat(maxRetries) {
            val count = journalEntryRepository.countByOrganizationId(organizationId)
            val entryNumber = "JE-${(count + 1).toString().padStart(4, '0')}"
            try {
                return journalEntryRepository.save(buildEntry(entryNumber))
            } catch (e: DuplicateKeyException) {
                if (it == maxRetries - 1) {
                    throw IllegalStateException("Failed to generate unique entry number")
                }
            }
        }
        throw IllegalStateException("Failed to generate unique entry number")
    }
}
