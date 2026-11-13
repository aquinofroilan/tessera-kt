package com.aquinofroilan.tessera.exception

import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import java.net.URI
import java.util.UUID

data class FieldViolation(
    val field: String,
    val code: String,
    val message: String,
)

object ApiProblemDetails {
    fun create(
        status: HttpStatusCode,
        code: String,
        detail: String,
        instance: String? = null,
        errors: List<FieldViolation> = emptyList(),
        traceId: String? = currentTraceId(),
    ): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.type = URI.create("about:blank")
        problem.title = HttpStatus.resolve(status.value())?.reasonPhrase ?: "Request failed"
        instance?.takeIf { it.isNotBlank() }?.let { problem.instance = URI.create(it) }
        problem.setProperty("code", code)
        if (errors.isNotEmpty()) problem.setProperty("errors", errors)
        problem.setProperty("traceId", traceId ?: UUID.randomUUID().toString())
        return problem
    }

    fun enrich(
        problem: ProblemDetail,
        status: HttpStatusCode,
        instance: String?,
    ): ProblemDetail {
        problem.status = status.value()
        if (problem.title.isNullOrBlank()) {
            problem.title = HttpStatus.resolve(status.value())?.reasonPhrase ?: "Request failed"
        }
        if (problem.type == null) problem.type = URI.create("about:blank")
        if (problem.instance == null) instance?.takeIf { it.isNotBlank() }?.let { problem.instance = URI.create(it) }
        if (problem.properties?.containsKey("code") != true) {
            problem.setProperty("code", defaultCode(status))
        }
        if (problem.properties?.containsKey("traceId") != true) {
            problem.setProperty("traceId", currentTraceId() ?: UUID.randomUUID().toString())
        }
        return problem
    }

    fun defaultCode(status: HttpStatusCode): String =
        when (status.value()) {
            400 -> "INVALID_REQUEST"
            401 -> "AUTHENTICATION_REQUIRED"
            403 -> "ACCESS_DENIED"
            404 -> "RESOURCE_NOT_FOUND"
            409 -> "RESOURCE_CONFLICT"
            422 -> "VALIDATION_FAILED"
            in 500..599 -> "INTERNAL_ERROR"
            else -> "HTTP_ERROR"
        }

    fun currentTraceId(): String? = MDC.get("traceId") ?: MDC.get("trace_id")
}
