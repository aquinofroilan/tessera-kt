package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.domain.project.controller.TimeEntryController
import com.aquinofroilan.tessera.domain.project.dto.CreateTimeEntryRequest
import com.aquinofroilan.tessera.domain.project.dto.UpdateTimeEntryRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * GraphQL bridge for project time entries (timesheets), delegating to the REST
 * controller via the shared JSON-scalar pass-through. Authorization mirrors the
 * REST endpoints (`projects:read`/`projects:write`/`projects:approve`).
 */
@Controller
class TimeEntryGraphqlController(
    private val timeEntryController: TimeEntryController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun timeEntries(
        @Argument employeeId: java.util.UUID?,
        @Argument projectId: java.util.UUID?,
        @Argument status: String?,
    ): Any = support.unwrap(timeEntryController.listTimeEntries(support.orgId(), employeeId, projectId, status))

    @QueryMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun timeEntry(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(timeEntryController.getTimeEntry(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun createTimeEntry(
        @Argument input: Any,
    ): Any = support.unwrap(timeEntryController.createTimeEntry(support.orgId(), support.toRequest<CreateTimeEntryRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun updateTimeEntry(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(timeEntryController.updateTimeEntry(support.orgId(), id, support.toRequest<UpdateTimeEntryRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun submitTimeEntry(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(timeEntryController.submitTimeEntry(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:approve')")
    fun approveTimeEntry(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(timeEntryController.approveTimeEntry(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:approve')")
    fun rejectTimeEntry(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(timeEntryController.rejectTimeEntry(support.orgId(), id))
}
