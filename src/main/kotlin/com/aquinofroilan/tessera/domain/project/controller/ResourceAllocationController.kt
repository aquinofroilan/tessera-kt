package com.aquinofroilan.tessera.domain.project.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.project.dto.CreateResourceAllocationRequest
import com.aquinofroilan.tessera.domain.project.dto.ResourceAllocationResponse
import com.aquinofroilan.tessera.domain.project.dto.UpdateResourceAllocationRequest
import com.aquinofroilan.tessera.domain.project.service.ResourceAllocationService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}/allocations")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ResourceAllocationController(
    private val allocationService: ResourceAllocationService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun createAllocation(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: UUID,
        @Valid @RequestBody request: CreateResourceAllocationRequest,
    ): ResponseEntity<ResourceAllocationResponse> {
        val created = allocationService.createAllocation(projectId, request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ResourceAllocationResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun listAllocations(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: UUID,
    ): ResponseEntity<List<ResourceAllocationResponse>> {
        val allocations = allocationService.getAllocationsForProject(projectId, orgId)
        return ResponseEntity.ok(allocations.map { ResourceAllocationResponse.from(it) })
    }

    @GetMapping("/{allocationId}")
    @PreAuthorize("hasAuthority('projects:read')")
    fun getAllocation(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: UUID,
        @PathVariable allocationId: UUID,
    ): ResponseEntity<ResourceAllocationResponse> {
        val allocation = allocationService.getAllocation(projectId, allocationId, orgId)
        return ResponseEntity.ok(ResourceAllocationResponse.from(allocation))
    }

    @PatchMapping("/{allocationId}")
    @PreAuthorize("hasAuthority('projects:write')")
    fun updateAllocation(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: UUID,
        @PathVariable allocationId: UUID,
        @Valid @RequestBody request: UpdateResourceAllocationRequest,
    ): ResponseEntity<ResourceAllocationResponse> {
        val updated = allocationService.updateAllocation(projectId, allocationId, request, orgId)
        return ResponseEntity.ok(ResourceAllocationResponse.from(updated))
    }

    @DeleteMapping("/{allocationId}")
    @PreAuthorize("hasAuthority('projects:write')")
    fun deleteAllocation(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: UUID,
        @PathVariable allocationId: UUID,
    ): ResponseEntity<Void> {
        allocationService.deleteAllocation(projectId, allocationId, orgId)
        return ResponseEntity.noContent().build()
    }
}
