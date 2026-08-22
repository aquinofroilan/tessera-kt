package com.aquinofroilan.tessera.domain.mfg.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.BomResponse
import com.aquinofroilan.tessera.domain.finance.dto.CreateBomRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateBomRequest
import com.aquinofroilan.tessera.domain.mfg.model.BomStatus
import com.aquinofroilan.tessera.domain.mfg.service.BillOfMaterialsService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
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
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/mfg/boms")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class BillOfMaterialsController(
    private val bomService: BillOfMaterialsService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('mfg:write')")
    fun create(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateBomRequest,
    ): ResponseEntity<Any> {
        val bom = bomService.createBom(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(BomResponse.from(bom))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('mfg:read')")
    fun list(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) productId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val parsed =
            if (status != null) {
                try {
                    BomStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        val boms = bomService.listBoms(orgId, parsed, productId)
        return ResponseEntity.ok(boms.map { BomResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun get(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(BomResponse.from(bomService.getBom(id, orgId)))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun update(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateBomRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(BomResponse.from(bomService.updateBom(id, request, orgId)))

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('mfg:approve')")
    fun activate(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @RequestParam(required = false, defaultValue = "false") makeDefault: Boolean,
    ): ResponseEntity<Any> = ResponseEntity.ok(BomResponse.from(bomService.activateBom(id, orgId, userId, makeDefault)))

    @PostMapping("/{id}/obsolete")
    @PreAuthorize("hasAuthority('mfg:approve')")
    fun obsolete(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(BomResponse.from(bomService.obsoleteBom(id, orgId, userId)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun delete(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        bomService.deleteBom(id, orgId)
        return ResponseEntity.noContent().build()
    }
}
