package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.CreateJobPostingRequest
import com.aquinofroilan.tessera.domain.hr.dto.JobPostingResponse
import com.aquinofroilan.tessera.domain.hr.dto.UpdateJobPostingRequest
import com.aquinofroilan.tessera.domain.hr.model.JobPostingStatus
import com.aquinofroilan.tessera.domain.hr.service.JobPostingService
import com.aquinofroilan.tessera.security.AuthenticationContext
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
@RequestMapping("/api/v1/hr/recruitment/jobs")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class JobPostingController(
    private val jobPostingService: JobPostingService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun create(
        @Valid @RequestBody request: CreateJobPostingRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        val p = jobPostingService.createPosting(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(JobPostingResponse.from(p))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr-recruitment:read')")
    fun list(
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val parsed =
            if (status != null) {
                try {
                    JobPostingStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(jobPostingService.listPostings(orgId, parsed).map { JobPostingResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr-recruitment:read')")
    fun get(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.getPosting(id, orgId)))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateJobPostingRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.updatePosting(id, request, orgId)))
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasAuthority('hr-recruitment:approve')")
    fun open(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.openPosting(id, orgId)))
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('hr-recruitment:approve')")
    fun close(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.closePosting(id, orgId)))
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun cancel(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.cancelPosting(id, orgId)))
    }
}
