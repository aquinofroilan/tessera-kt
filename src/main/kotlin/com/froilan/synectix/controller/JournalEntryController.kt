package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.CreateJournalEntryRequest
import com.froilan.synectix.dto.JournalEntryLineResponse
import com.froilan.synectix.dto.JournalEntryResponse
import com.froilan.synectix.dto.VoidJournalEntryRequest
import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryStatus
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.service.JournalEntryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.Locale

@RestController
@RequestMapping("/finance/journal")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class JournalEntryController(
    private val journalEntryService: JournalEntryService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('journal:create')")
    fun createJournalEntry(
        @Valid @RequestBody request: CreateJournalEntryRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: "api-key"

        return try {
            val entry = journalEntryService.createJournalEntry(request, orgId, createdBy)
            ResponseEntity.status(HttpStatus.CREATED).body(entry.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Failed to create journal entry")))
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('journal:read')")
    fun listJournalEntries(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) startDate: LocalDate?,
        @RequestParam(required = false) endDate: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val entryStatus =
            if (status != null) {
                try {
                    JournalEntryStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(
                        mapOf("error" to "Invalid status '$status'"),
                    )
                }
            } else {
                null
            }

        val entries = journalEntryService.listJournalEntries(orgId, entryStatus, startDate, endDate)
        return ResponseEntity.ok(entries.map { it.toResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('journal:read')")
    fun getJournalEntry(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        return try {
            val entry = journalEntryService.getJournalEntry(id, orgId)
            ResponseEntity.ok(entry.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (e.message ?: "Journal entry not found")))
        }
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('journal:post')")
    fun postJournalEntry(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        return try {
            val entry = journalEntryService.postJournalEntry(id, orgId)
            ResponseEntity.ok(entry.toResponse())
        } catch (e: IllegalArgumentException) {
            val status = if (e.message == "Journal entry not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity.status(status).body(mapOf("error" to (e.message ?: "Failed to post journal entry")))
        }
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('journal:void')")
    fun voidJournalEntry(
        @PathVariable id: String,
        @Valid @RequestBody request: VoidJournalEntryRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        return try {
            val entry = journalEntryService.voidJournalEntry(id, orgId, request.reason)
            ResponseEntity.ok(entry.toResponse())
        } catch (e: IllegalArgumentException) {
            val status = if (e.message == "Journal entry not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity.status(status).body(mapOf("error" to (e.message ?: "Failed to void journal entry")))
        }
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAuthority('journal:read')")
    fun getTrialBalance(
        @RequestParam(required = false) asOfDate: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val trialBalance = journalEntryService.getTrialBalance(orgId, asOfDate)
        return ResponseEntity.ok(trialBalance)
    }

    private fun JournalEntry.toResponse() =
        JournalEntryResponse(
            id = id,
            entryNumber = entryNumber,
            date = date.toString(),
            description = description,
            organizationId = organizationId,
            status = status.name,
            source = source.name,
            sourceReference = sourceReference,
            lines =
                lines.map { line ->
                    JournalEntryLineResponse(
                        accountId = line.accountId,
                        accountCode = line.accountCode,
                        accountName = line.accountName,
                        debit = line.debit,
                        credit = line.credit,
                        description = line.description,
                    )
                },
            createdBy = createdBy,
            postedAt = postedAt?.toString(),
            voidedAt = voidedAt?.toString(),
            voidReason = voidReason,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
