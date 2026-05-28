package com.froilan.synectix.graphql

import com.froilan.synectix.exception.AuthenticationException
import com.froilan.synectix.exception.ResourceNotFoundException
import jakarta.validation.Validator
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class GraphqlBridgeSupport(
    private val objectMapper: ObjectMapper,
    private val validator: Validator,
) {
    fun <T : Any> toRequest(
        input: Any,
        type: Class<T>,
    ): T {
        val request = objectMapper.convertValue(input, type)
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            val message = violations.joinToString(", ") { it.message }
            throw IllegalArgumentException(message)
        }
        return request
    }

    fun unwrap(response: ResponseEntity<*>): Any {
        if (response.statusCode.is2xxSuccessful) {
            return response.body ?: emptyMap<String, Any>()
        }

        val message =
            when (val body = response.body) {
                is Map<*, *> -> body["error"]?.toString() ?: body["message"]?.toString()
                else -> body?.toString()
            } ?: "Request failed with status ${response.statusCode.value()}"

        when (response.statusCode.value()) {
            401 -> throw AuthenticationException(message)
            404 -> throw ResourceNotFoundException(message)
            422 -> throw IllegalStateException(message)
            in 500..599 -> throw UpstreamServiceException(message)
            else -> throw IllegalArgumentException(message)
        }
    }
}

inline fun <reified T : Any> GraphqlBridgeSupport.toRequest(input: Any): T = toRequest(input, T::class.java)

class UpstreamServiceException(
    message: String,
) : RuntimeException(message)
