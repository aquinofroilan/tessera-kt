package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.domain.hr.controller.AttendanceController
import com.aquinofroilan.tessera.domain.hr.dto.ClockRequest
import com.aquinofroilan.tessera.domain.hr.dto.RecordAttendanceRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDate

/**
 * GraphQL bridge for the HR attendance routes — clock-in/out, manual entry and
 * timesheets — delegating to the REST controller via the shared JSON-scalar
 * pass-through. Authorization mirrors the REST endpoints (`hr:read`/`hr:write`).
 */
@Controller
class AttendanceGraphqlController(
    private val attendanceController: AttendanceController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun attendance(
        @Argument employeeId: java.util.UUID?,
        @Argument from: String?,
        @Argument to: String?,
    ): Any =
        support.unwrap(
            attendanceController.listTimesheet(support.orgId(), employeeId, from?.let(LocalDate::parse), to?.let(LocalDate::parse)),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun attendanceRecord(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(attendanceController.getAttendance(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun clockIn(
        @Argument input: Any,
    ): Any = support.unwrap(attendanceController.clockIn(support.orgId(), support.toRequest<ClockRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun clockOut(
        @Argument input: Any,
    ): Any = support.unwrap(attendanceController.clockOut(support.orgId(), support.toRequest<ClockRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun recordAttendance(
        @Argument input: Any,
    ): Any = support.unwrap(attendanceController.recordAttendance(support.orgId(), support.toRequest<RecordAttendanceRequest>(input)))
}
