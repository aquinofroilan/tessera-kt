package com.froilan.synectix.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(e: ResourceNotFoundException): ResponseEntity<Map<String, String?>> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to e.message))

    @ExceptionHandler(BusinessRuleException::class)
    fun handleBusinessRule(e: BusinessRuleException): ResponseEntity<Map<String, String?>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to e.message))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String?>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to e.message))

    @ExceptionHandler(IllegalStateException::class)
    fun handleUnprocessable(e: IllegalStateException): ResponseEntity<Map<String, String?>> =
        ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(mapOf("error" to e.message))
}
