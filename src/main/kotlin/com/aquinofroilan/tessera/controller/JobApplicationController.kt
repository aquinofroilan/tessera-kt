package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.AdvanceApplicationRequest
import com.aquinofroilan.tessera.dto.ApplicationResponse
import com.aquinofroilan.tessera.dto.CreateApplicationRequest
import com.aquinofroilan.tessera.dto.UpdateApplicationRequest
import com.aquinofroilan.tessera.model.JobApplicationStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.JobApplicationService
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
@RequestMapping("/hr/recruitment/applications")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class JobApplicationController(
    private val applicationService: JobApplicationService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun create(
        @Valid @RequestBody request: CreateApplicationRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        val a = applicationService.createApplication(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationResponse.from(a))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr-recruitment:read')")
    fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) jobPostingId: UUID?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
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
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ApplicationResponse.from(applicationService.getApplication(id, orgId)))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateApplicationRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ApplicationResponse.from(applicationService.updateApplication(id, request, orgId)))
    }

    @PostMapping("/{id}/advance")
    @PreAuthorize("hasAuthority('hr-recruitment:approve')")
    fun advance(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AdvanceApplicationRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ApplicationResponse.from(applicationService.advanceApplication(id, request, orgId)))
    }
}
