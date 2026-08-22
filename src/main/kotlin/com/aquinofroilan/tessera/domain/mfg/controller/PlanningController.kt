package com.aquinofroilan.tessera.domain.mfg.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.mfg.model.MpsStatus
import com.aquinofroilan.tessera.domain.mfg.service.CrpService
import com.aquinofroilan.tessera.domain.mfg.service.MpsService
import com.aquinofroilan.tessera.domain.mfg.service.MrpService
import com.aquinofroilan.tessera.domain.platform.dto.CreateMpsEntryRequest
import com.aquinofroilan.tessera.domain.platform.dto.MpsEntryResponse
import com.aquinofroilan.tessera.domain.platform.dto.UpdateMpsEntryRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/mfg/planning")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class PlanningController(
    private val mpsService: MpsService,
    private val mrpService: MrpService,
    private val crpService: CrpService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping("/mps")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun createMps(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateMpsEntryRequest,
    ): ResponseEntity<Any> {
        val entry = mpsService.create(request, orgId, userId.toString())
        return ResponseEntity.status(HttpStatus.CREATED).body(MpsEntryResponse.from(entry))
    }

    @GetMapping("/mps")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun listMps(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
        val parsed =
            if (status != null) {
                try {
                    MpsStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(mpsService.list(orgId, parsed).map { MpsEntryResponse.from(it) })
    }

    @GetMapping("/mps/{id}")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun getMps(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(MpsEntryResponse.from(mpsService.get(id, orgId)))

    @PutMapping("/mps/{id}")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun updateMps(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateMpsEntryRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(MpsEntryResponse.from(mpsService.update(id, request, orgId)))

    @DeleteMapping("/mps/{id}")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun deleteMps(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        mpsService.delete(id, orgId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/mrp/run")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun runMrp(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) horizonEnd: LocalDate?,
    ): ResponseEntity<Any> = ResponseEntity.ok(mrpService.run(orgId, horizonEnd))

    @PostMapping("/crp/run")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun runCrp(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) horizonEnd: LocalDate?,
        @RequestParam(required = false, defaultValue = "8") capacityHoursPerWorkingDay: BigDecimal,
    ): ResponseEntity<Any> = ResponseEntity.ok(crpService.run(orgId, horizonEnd, capacityHoursPerWorkingDay))
}
