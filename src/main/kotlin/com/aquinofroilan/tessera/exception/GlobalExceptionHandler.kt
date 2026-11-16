package com.aquinofroilan.tessera.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler(), ResponseBodyAdvice<Any> {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(
        e: ResourceNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<Any> =
        response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", e.message ?: "Resource not found", request)

    @ExceptionHandler(BusinessRuleException::class)
    fun handleBusinessRule(
        e: BusinessRuleException,
        request: HttpServletRequest,
    ): ResponseEntity<Any> =
        response(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", e.message ?: "Invalid request", request)

    @ExceptionHandler(UnprocessableRequestException::class)
    fun handleUnprocessable(
        e: UnprocessableRequestException,
        request: HttpServletRequest,
    ): ResponseEntity<Any> =
        response(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", e.message ?: "Request cannot be processed", request)

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(
        e: ConflictException,
        request: HttpServletRequest,
    ): ResponseEntity<Any> =
        response(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", e.message ?: "Request conflicts with the current resource state", request)

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(
        e: AuthenticationException,
        request: HttpServletRequest,
    ): ResponseEntity<Any> =
        response(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", e.message ?: "Authentication failed", request)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(
        e: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<Any> =
        response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.message ?: "Invalid request", request)

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        e: IllegalStateException,
        request: HttpServletRequest,
    ): ResponseEntity<Any> =
        response(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", e.message ?: "Request cannot be processed", request)

    @ExceptionHandler(FeatureNotEnabledException::class)
    fun handleFeatureNotEnabled(
        e: FeatureNotEnabledException,
        request: HttpServletRequest,
    ): ResponseEntity<Any> =
        response(HttpStatus.FORBIDDEN, "FEATURE_NOT_ENABLED", e.message ?: "Feature is not enabled", request)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        e: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<Any> {
        val traceId = ApiProblemDetails.currentTraceId() ?: UUID.randomUUID().toString()
        log.error("Unhandled request failure traceId={}", traceId, e)
        return response(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "An unexpected error occurred.",
            request,
            traceId,
        )
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors =
            ex.bindingResult.allErrors.map { error ->
                FieldViolation(
                    field = (error as? FieldError)?.field ?: "_global",
                    code = error.codes?.firstOrNull() ?: "INVALID_VALUE",
                    message = error.defaultMessage ?: "Invalid value",
                )
            }
        val problem =
            ApiProblemDetails.create(
                status = status,
                code = "VALIDATION_FAILED",
                detail = "One or more request fields are invalid.",
                instance = requestPath(request),
                errors = errors,
            )
        return handleExceptionInternal(ex, problem, headers, status, request)
    }

    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors =
            ex.allErrors.map { error ->
                FieldViolation(
                    field = (error as? FieldError)?.field ?: "_global",
                    code = error.codes?.firstOrNull() ?: "INVALID_VALUE",
                    message = error.defaultMessage ?: "Invalid value",
                )
            }
        val problem =
            ApiProblemDetails.create(
                status = status,
                code = "VALIDATION_FAILED",
                detail = "One or more request parameters are invalid.",
                instance = requestPath(request),
                errors = errors,
            )
        return handleExceptionInternal(ex, problem, headers, status, request)
    }

    override fun createResponseEntity(
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val problem =
            (body as? ProblemDetail)
                ?: ApiProblemDetails.create(
                    status = statusCode,
                    code = ApiProblemDetails.defaultCode(statusCode),
                    detail = HttpStatus.resolve(statusCode.value())?.reasonPhrase ?: "Request failed",
                    instance = requestPath(request),
                )
        ApiProblemDetails.enrich(problem, statusCode, requestPath(request))
        if (headers.contentType == null) headers.contentType = MediaType.APPLICATION_PROBLEM_JSON
        return super.createResponseEntity(problem, headers, statusCode, request)
    }

    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean = true

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        val legacyError = body as? Map<*, *> ?: return body
        if (legacyError.keys != setOf("error")) return body

        val status =
            (response as? ServletServerHttpResponse)?.servletResponse?.status
                ?.takeIf { it in 400..599 }
                ?.let(HttpStatusCode::valueOf)
                ?: HttpStatus.BAD_REQUEST
        response.headers.contentType = MediaType.APPLICATION_PROBLEM_JSON
        return ApiProblemDetails.create(
            status = status,
            code = ApiProblemDetails.defaultCode(status),
            detail = legacyError["error"]?.toString() ?: "Request failed",
            instance = request.uri.path,
        )
    }

    private fun response(
        status: HttpStatus,
        code: String,
        detail: String,
        request: HttpServletRequest,
        traceId: String? = null,
    ): ResponseEntity<Any> {
        val problem = ApiProblemDetails.create(status, code, detail, request.requestURI, traceId = traceId)
        return ResponseEntity
            .status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem)
    }

    private fun requestPath(request: WebRequest): String? =
        (request as? ServletWebRequest)?.request?.requestURI
}
