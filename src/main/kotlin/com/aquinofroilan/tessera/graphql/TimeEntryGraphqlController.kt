package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.controller.TimeEntryController
import com.aquinofroilan.tessera.dto.CreateTimeEntryRequest
import com.aquinofroilan.tessera.dto.UpdateTimeEntryRequest
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
    ): Any = support.unwrap(timeEntryController.listTimeEntries(employeeId, projectId, status))

    @QueryMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun timeEntry(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(timeEntryController.getTimeEntry(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun createTimeEntry(
        @Argument input: Any,
    ): Any = support.unwrap(timeEntryController.createTimeEntry(support.toRequest<CreateTimeEntryRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun updateTimeEntry(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(timeEntryController.updateTimeEntry(id, support.toRequest<UpdateTimeEntryRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun submitTimeEntry(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(timeEntryController.submitTimeEntry(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:approve')")
    fun approveTimeEntry(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(timeEntryController.approveTimeEntry(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:approve')")
    fun rejectTimeEntry(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(timeEntryController.rejectTimeEntry(id))
}
