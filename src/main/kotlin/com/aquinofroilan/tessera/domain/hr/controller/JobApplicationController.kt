package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.AdvanceApplicationRequest
import com.aquinofroilan.tessera.domain.hr.dto.ApplicationResponse
import com.aquinofroilan.tessera.domain.hr.dto.CreateApplicationRequest
import com.aquinofroilan.tessera.domain.hr.dto.UpdateApplicationRequest
import com.aquinofroilan.tessera.domain.hr.model.JobApplicationStatus
import com.aquinofroilan.tessera.domain.hr.service.JobApplicationService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/hr/recruitment/applications")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class JobApplicationController(
    private val applicationService: JobApplicationService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun create(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateApplicationRequest,
    ): ResponseEntity<Any> {
        val a = applicationService.createApplication(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationResponse.from(a))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr-recruitment:read')")
    fun list(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) jobPostingId: UUID?,
    ): ResponseEntity<Any> {
        val parsed =
            if (status != null) {
                try {
                    JobApplicationStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(
            applicationService.listApplications(orgId, parsed, jobPostingId).map { ApplicationResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr-recruitment:read')")
    fun get(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(ApplicationResponse.from(applicationService.getApplication(id, orgId)))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun update(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateApplicationRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(ApplicationResponse.from(applicationService.updateApplication(id, request, orgId)))

    @PostMapping("/{id}/advance")
    @PreAuthorize("hasAuthority('hr-recruitment:approve')")
    fun advance(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: AdvanceApplicationRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(ApplicationResponse.from(applicationService.advanceApplication(id, request, orgId)))
}
