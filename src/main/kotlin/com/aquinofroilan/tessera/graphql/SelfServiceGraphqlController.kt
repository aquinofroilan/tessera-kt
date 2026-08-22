package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.domain.hr.controller.SelfServiceController
import com.aquinofroilan.tessera.domain.platform.dto.SubmitSelfLeaveRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDate

/**
 * GraphQL bridge for the employee self-service routes (`/hr/me`), delegating to
 * the REST controller via the shared JSON-scalar pass-through. Open to any
 * authenticated user; the controller resolves the caller's own employee record,
 * so a user can only ever see or act on their own data.
 */
@Controller
class SelfServiceGraphqlController(
    private val selfServiceController: SelfServiceController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun me(): Any = support.unwrap(selfServiceController.myProfile())

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myLeaveRequests(): Any = support.unwrap(selfServiceController.myLeaveRequests())

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myLeaveBalance(
        @Argument leaveTypeId: java.util.UUID,
        @Argument year: Int?,
    ): Any = support.unwrap(selfServiceController.myLeaveBalance(leaveTypeId, year))

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myCompensation(): Any = support.unwrap(selfServiceController.myCompensationHistory())

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myCurrentCompensation(
        @Argument asOf: String?,
    ): Any = support.unwrap(selfServiceController.myCurrentCompensation(asOf?.let(LocalDate::parse)))

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun submitMyLeave(
        @Argument input: Any,
    ): Any = support.unwrap(selfServiceController.submitLeave(support.toRequest<SubmitSelfLeaveRequest>(input)))
}
