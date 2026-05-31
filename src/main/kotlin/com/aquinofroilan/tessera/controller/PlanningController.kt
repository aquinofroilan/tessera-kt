package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateMpsEntryRequest
import com.aquinofroilan.tessera.dto.MpsEntryResponse
import com.aquinofroilan.tessera.dto.UpdateMpsEntryRequest
import com.aquinofroilan.tessera.model.MpsStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.CrpService
import com.aquinofroilan.tessera.service.MpsService
import com.aquinofroilan.tessera.service.MrpService
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

@RestController
@RequestMapping("/mfg")
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
        @Valid @RequestBody request: CreateMpsEntryRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"
        val entry = mpsService.create(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(MpsEntryResponse.from(entry))
    }

    @GetMapping("/mps")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun listMps(
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
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
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(MpsEntryResponse.from(mpsService.get(id, orgId)))
    }

    @PutMapping("/mps/{id}")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun updateMps(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateMpsEntryRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(MpsEntryResponse.from(mpsService.update(id, request, orgId)))
    }

    @DeleteMapping("/mps/{id}")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun deleteMps(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        mpsService.delete(id, orgId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/mrp/run")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun runMrp(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) horizonEnd: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(mrpService.run(orgId, horizonEnd))
    }

    @PostMapping("/crp/run")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun runCrp(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) horizonEnd: LocalDate?,
        @RequestParam(required = false, defaultValue = "8") capacityHoursPerWorkingDay: BigDecimal,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(crpService.run(orgId, horizonEnd, capacityHoursPerWorkingDay))
    }
}
