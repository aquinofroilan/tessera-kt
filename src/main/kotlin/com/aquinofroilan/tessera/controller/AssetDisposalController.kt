package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.AssetDisposalResponse
import com.aquinofroilan.tessera.dto.CreateAssetDisposalRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.asset.AssetDisposalService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/assets/disposals")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AssetDisposalController(
    private val assetDisposalService: AssetDisposalService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('assets:write')")
    fun createDisposal(
        @Valid @RequestBody request: CreateAssetDisposalRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val date = request.disposalDate ?: throw BusinessRuleException("Disposal date is required")
        val type = request.disposalType ?: throw BusinessRuleException("Disposal type is required")
        val created =
            assetDisposalService.createDisposal(
                organizationId = orgId,
                assetId = request.assetId,
                disposalType = type,
                disposalDate = date,
                proceeds = request.proceeds,
                gainLossAccountId = request.gainLossAccountId,
                cashAccountId = request.cashAccountId,
                notes = request.notes,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(AssetDisposalResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('assets:read')")
    fun listDisposals(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(
            assetDisposalService.listDisposals(orgId).map { AssetDisposalResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('assets:read')")
    fun getDisposal(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(AssetDisposalResponse.from(assetDisposalService.getDisposal(id, orgId)))
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('assets:approve')")
    fun postDisposal(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(
            AssetDisposalResponse.from(assetDisposalService.postDisposal(id, orgId, userId)),
        )
    }
}
