package com.loom.synectix.controller

import com.loom.synectix.annotation.LogLevel
import com.loom.synectix.annotation.Loggable
import com.loom.synectix.dto.CreateJournalEntryRequest
import com.loom.synectix.dto.JournalEntryLineResponse
import com.loom.synectix.dto.JournalEntryResponse
import com.loom.synectix.dto.VoidJournalEntryRequest
import com.loom.synectix.model.JournalEntry
import com.loom.synectix.model.JournalEntryStatus
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.service.JournalEntryService
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

        val entry = journalEntryService.createJournalEntry(request, orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(entry.toResponse())
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

        val entry = journalEntryService.getJournalEntry(id, orgId)
        return ResponseEntity.ok(entry.toResponse())
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('journal:post')")
    fun postJournalEntry(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val entry = journalEntryService.postJournalEntry(id, orgId)
        return ResponseEntity.ok(entry.toResponse())
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('journal:void')")
    fun voidJournalEntry(
        @PathVariable id: String,
        @Valid @RequestBody request: VoidJournalEntryRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val entry = journalEntryService.voidJournalEntry(id, orgId, request.reason)
        return ResponseEntity.ok(entry.toResponse())
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
