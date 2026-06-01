package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CompleteInterviewRequest
import com.aquinofroilan.tessera.dto.CreateInterviewRequest
import com.aquinofroilan.tessera.dto.InterviewResponse
import com.aquinofroilan.tessera.dto.RescheduleInterviewRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.InterviewService
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

@RestController
@RequestMapping("/hr/recruitment/interviews")
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
        val userId = authContext.userId() ?: "api-key"
        val i = interviewService.scheduleInterview(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(InterviewResponse.from(i))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr-recruitment:read')")
    fun listForApplication(
        @RequestParam applicationId: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(
            interviewService.listInterviewsForApplication(orgId, applicationId).map { InterviewResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr-recruitment:read')")
    fun get(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(InterviewResponse.from(interviewService.getInterview(id, orgId)))
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun reschedule(
        @PathVariable id: String,
        @Valid @RequestBody request: RescheduleInterviewRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(InterviewResponse.from(interviewService.rescheduleInterview(id, request, orgId)))
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('hr-recruitment:approve')")
    fun complete(
        @PathVariable id: String,
        @Valid @RequestBody request: CompleteInterviewRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(InterviewResponse.from(interviewService.completeInterview(id, request, orgId)))
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('hr-recruitment:write')")
    fun cancel(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(InterviewResponse.from(interviewService.cancelInterview(id, orgId)))
    }
}
