package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreatePaymentRunRequest
import com.aquinofroilan.tessera.dto.PaymentRunResponse
import com.aquinofroilan.tessera.model.PaymentRunStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.PaymentRunService
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
import java.util.Locale

@RestController
@RequestMapping("/finance/payment-runs")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class PaymentRunController(
    private val paymentRunService: PaymentRunService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('bank:write')")
    fun create(
        @Valid @RequestBody request: CreatePaymentRunRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        val r = paymentRunService.createPaymentRun(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentRunResponse.from(r))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('bank:read')")
    fun list(
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val parsed =
            if (status != null) {
                try {
                    PaymentRunStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(paymentRunService.listPaymentRuns(orgId, parsed).map { PaymentRunResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('bank:read')")
    fun get(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(PaymentRunResponse.from(paymentRunService.getPaymentRun(id, orgId)))
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('bank:approve')")
    fun approve(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(PaymentRunResponse.from(paymentRunService.approvePaymentRun(id, orgId, userId)))
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('bank:approve')")
    fun execute(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(PaymentRunResponse.from(paymentRunService.executePaymentRun(id, orgId, userId)))
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('bank:write')")
    fun cancel(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(PaymentRunResponse.from(paymentRunService.cancelPaymentRun(id, orgId, userId)))
    }
}
