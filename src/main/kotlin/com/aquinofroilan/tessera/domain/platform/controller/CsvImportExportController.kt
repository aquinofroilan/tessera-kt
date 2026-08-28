package com.aquinofroilan.tessera.domain.platform.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.platform.service.CsvImportExportService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/io/csv")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class CsvImportExportController(
    private val csvService: CsvImportExportService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping("/products/export")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun exportProducts(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<ByteArray> = csv(csvService.exportProducts(orgId), "products.csv")

    @GetMapping("/customers/export")
    @PreAuthorize("hasAuthority('sales:read')")
    fun exportCustomers(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<ByteArray> = csv(csvService.exportCustomers(orgId), "customers.csv")

    @GetMapping("/vendors/export")
    @PreAuthorize("hasAuthority('procurement:read')")
    fun exportVendors(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<ByteArray> = csv(csvService.exportVendors(orgId), "vendors.csv")

    @PostMapping("/products/import")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun importProducts(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<Any> = ResponseEntity.ok(csvService.importProducts(file.bytes, orgId))

    @PostMapping("/customers/import")
    @PreAuthorize("hasAuthority('sales:write')")
    fun importCustomers(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<Any> = ResponseEntity.ok(csvService.importCustomers(file.bytes, orgId))

    private fun csv(
        bytes: ByteArray,
        filename: String,
    ): ResponseEntity<ByteArray> =
        ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(bytes)
}
