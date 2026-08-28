package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.AttendanceResponse
import com.aquinofroilan.tessera.domain.hr.dto.ClockRequest
import com.aquinofroilan.tessera.domain.hr.dto.RecordAttendanceRequest
import com.aquinofroilan.tessera.domain.hr.service.AttendanceService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
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
import java.util.UUID

@RestController
@RequestMapping("/api/v1/hr/attendance")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AttendanceController(
    private val attendanceService: AttendanceService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping("/clock-in")
    @PreAuthorize("hasAuthority('hr:write')")
    fun clockIn(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: ClockRequest,
    ): ResponseEntity<Any> {
        val record = attendanceService.clockIn(request.employeeId, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(AttendanceResponse.from(record))
    }

    @PostMapping("/clock-out")
    @PreAuthorize("hasAuthority('hr:write')")
    fun clockOut(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: ClockRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(AttendanceResponse.from(attendanceService.clockOut(request.employeeId, orgId)))

    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun recordAttendance(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: RecordAttendanceRequest,
    ): ResponseEntity<Any> {
        val record = attendanceService.recordAttendance(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(AttendanceResponse.from(record))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listTimesheet(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) employeeId: java.util.UUID?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            attendanceService.listTimesheet(orgId, employeeId, from, to).map { AttendanceResponse.from(it) },
        )

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getAttendance(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(AttendanceResponse.from(attendanceService.getAttendance(id, orgId)))
}
