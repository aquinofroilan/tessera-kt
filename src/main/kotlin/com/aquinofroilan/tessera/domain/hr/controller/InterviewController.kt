package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.CompleteInterviewRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateInterviewRequest
import com.aquinofroilan.tessera.domain.hr.dto.InterviewResponse
import com.aquinofroilan.tessera.domain.hr.dto.RescheduleInterviewRequest
import com.aquinofroilan.tessera.domain.hr.service.InterviewService
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
import java.util.UUID

@RestController
@RequestMapping("/api/v1/hr/recruitment/interviews")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class InterviewController(
    private val interviewService: InterviewService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun schedule(
        @Valid @RequestBody request: CreateInterviewRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        val i = interviewService.scheduleInterview(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(InterviewResponse.from(i))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr-recruitment:read')")
    fun listForApplication(
        @RequestParam applicationId: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(
            interviewService.listInterviewsForApplication(orgId, applicationId).map { InterviewResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr-recruitment:read')")
    fun get(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(InterviewResponse.from(interviewService.getInterview(id, orgId)))
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun reschedule(
        @PathVariable id: UUID,
        @Valid @RequestBody request: RescheduleInterviewRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(InterviewResponse.from(interviewService.rescheduleInterview(id, request, orgId)))
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('hr-recruitment:approve')")
    fun complete(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CompleteInterviewRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(InterviewResponse.from(interviewService.completeInterview(id, request, orgId)))
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun cancel(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(InterviewResponse.from(interviewService.cancelInterview(id, orgId)))
    }
}
