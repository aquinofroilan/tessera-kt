package com.aquinofroilan.tessera.domain.platform.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.platform.dto.AttachmentResponse
import com.aquinofroilan.tessera.domain.platform.service.AttachmentService
import com.aquinofroilan.tessera.security.AuthenticationContext
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/platform/attachments")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AttachmentController(
    private val attachmentService: AttachmentService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('attachment:write')")
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestParam entityType: String,
        @RequestParam entityId: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val a = attachmentService.upload(file, entityType, entityId, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(AttachmentResponse.from(a))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('attachment:read')")
    fun list(
        @RequestParam entityType: String,
        @RequestParam entityId: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(
            attachmentService.listForEntity(orgId, entityType, entityId).map { AttachmentResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('attachment:read')")
    fun get(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(AttachmentResponse.from(attachmentService.getAttachment(id, orgId)))
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('attachment:read')")
    fun download(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<InputStreamResource> {
        val orgId = authContext.organizationId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val a = attachmentService.getAttachment(id, orgId)
        val resource = InputStreamResource(attachmentService.openStream(a))
        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${a.filename}\"")
            .header(HttpHeaders.CONTENT_LENGTH, a.sizeBytes.toString())
            .contentType(MediaType.parseMediaType(a.mimeType))
            .body(resource)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('attachment:write')")
    fun delete(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        attachmentService.delete(id, orgId)
        return ResponseEntity.noContent().build()
    }
}
