package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.CreateJournalEntryRequest
import com.aquinofroilan.tessera.domain.finance.dto.JournalEntryLineResponse
import com.aquinofroilan.tessera.domain.finance.dto.JournalEntryResponse
import com.aquinofroilan.tessera.domain.finance.dto.VoidJournalEntryRequest
import com.aquinofroilan.tessera.domain.finance.model.JournalEntry
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryStatus
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.security.AuthenticationContext
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
@RequestMapping("/api/v1/finance/journal")
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
        val createdBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")

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
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val entry = journalEntryService.getJournalEntry(id, orgId)
        return ResponseEntity.ok(entry.toResponse())
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('journal:post')")
    fun postJournalEntry(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val entry = journalEntryService.postJournalEntry(id, orgId)
        return ResponseEntity.ok(entry.toResponse())
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('journal:void')")
    fun voidJournalEntry(
        @PathVariable id: java.util.UUID,
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
