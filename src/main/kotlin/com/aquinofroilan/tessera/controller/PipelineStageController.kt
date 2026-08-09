package com.aquinofroilan.tessera.controller
import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreatePipelineStageRequest
import com.aquinofroilan.tessera.dto.PipelineStageResponse
import com.aquinofroilan.tessera.dto.UpdatePipelineStageRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.PipelineStageService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
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
@RequestMapping("/crm/pipeline-stages")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class PipelineStageController(
    private val pipelineStageService: PipelineStageService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('crm:write')")
    fun create(
        @Valid @RequestBody request: CreatePipelineStageRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val stage = pipelineStageService.createStage(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(PipelineStageResponse.from(stage))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('crm:read')")
    fun list(
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val stages = pipelineStageService.listStages(orgId, activeOnly)
        return ResponseEntity.ok(stages.map { PipelineStageResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:read')")
    fun get(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(PipelineStageResponse.from(pipelineStageService.getStage(id, orgId)))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:write')")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdatePipelineStageRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(PipelineStageResponse.from(pipelineStageService.updateStage(id, request, orgId)))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:write')")
    fun deactivate(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(PipelineStageResponse.from(pipelineStageService.deactivateStage(id, orgId)))
    }
}
