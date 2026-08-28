package com.aquinofroilan.tessera.config

import com.aquinofroilan.tessera.domain.organization.model.OrganizationStatus
import com.aquinofroilan.tessera.domain.organization.service.OrganizationLifecycleService
import com.aquinofroilan.tessera.security.AuthenticationContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.util.Optional

@Component
class OrganizationStatusInterceptor(
    private val authContext: AuthenticationContext,
    private val organizationLifecycleService: Optional<OrganizationLifecycleService>,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)

        if (isExemptPath(path)) {
            return true
        }

        val orgId = authContext.organizationId() ?: return true

        val authentication = SecurityContextHolder.getContext().authentication
        val isSuperAdmin =
            authentication?.authorities?.any {
                it.authority == "ROLE_SUPER_ADMIN"
            } ?: false

        if (isSuperAdmin) {
            return true
        }

        val lifecycleService = organizationLifecycleService.orElse(null) ?: return true
        val status = lifecycleService.getCachedStatus(orgId)

        if (status == OrganizationStatus.SUSPENDED) {
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write("""{"error":"Organization is suspended"}""")
            return false
        }

        if (status == OrganizationStatus.ARCHIVED) {
            val method = request.method.uppercase()
            if (method !in READ_ONLY_METHODS) {
                response.status = HttpServletResponse.SC_FORBIDDEN
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.writer.write("""{"error":"Organization is archived and read-only"}""")
                return false
            }
        }

        return true
    }

    private fun isExemptPath(path: String): Boolean =
        path.startsWith("/auth") ||
            path.startsWith("/api/v1/auth") ||
            path.startsWith("/health") ||
            path.startsWith("/actuator") ||
            path.startsWith("/graphiql") ||
            path.startsWith("/environment") ||
            path == "/api/v1/organization/status" ||
            path.startsWith("/api/v1/organization/status/") ||
            path == "/api/v1/organizations" ||
            path.startsWith("/api/v1/organizations/")

    companion object {
        private val READ_ONLY_METHODS = setOf("GET", "HEAD", "OPTIONS")
    }
}
