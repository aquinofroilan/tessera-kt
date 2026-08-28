package com.aquinofroilan.tessera.domain.crm.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.crm.dto.ContactResponse
import com.aquinofroilan.tessera.domain.crm.dto.CreateContactRequest
import com.aquinofroilan.tessera.domain.crm.dto.UpdateContactRequest
import com.aquinofroilan.tessera.domain.crm.service.ContactService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/crm/contacts")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ContactController(
    private val contactService: ContactService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('crm:write')")
    fun create(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateContactRequest,
    ): ResponseEntity<Any> {
        val contact = contactService.createContact(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ContactResponse.from(contact))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('crm:read')")
    fun list(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean,
        @RequestParam(required = false) customerId: UUID?,
    ): ResponseEntity<Any> {
        val contacts = contactService.listContacts(orgId, activeOnly, customerId)
        return ResponseEntity.ok(contacts.map { ContactResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:read')")
    fun get(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(ContactResponse.from(contactService.getContact(id, orgId)))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:write')")
    fun update(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateContactRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(ContactResponse.from(contactService.updateContact(id, request, orgId)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:write')")
    fun deactivate(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(ContactResponse.from(contactService.deactivateContact(id, orgId)))
}
