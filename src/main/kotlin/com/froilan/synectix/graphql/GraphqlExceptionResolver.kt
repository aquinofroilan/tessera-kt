package com.froilan.synectix.graphql

import com.froilan.synectix.exception.AuthenticationException
import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.exception.ResourceNotFoundException
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class GraphqlExceptionResolver : DataFetcherExceptionResolverAdapter() {
    override fun resolveToSingleError(
        ex: Throwable,
        env: DataFetchingEnvironment,
    ): GraphQLError? =
        when (ex) {
            is ResourceNotFoundException -> error(env, ex.message ?: "Resource not found", ErrorType.NOT_FOUND)
            is BusinessRuleException, is IllegalArgumentException -> error(env, ex.message ?: "Invalid request", ErrorType.BAD_REQUEST)
            is AuthenticationException -> error(env, ex.message ?: "Authentication failed", ErrorType.UNAUTHORIZED)
            is IllegalStateException -> error(env, ex.message ?: "Unable to process request", ErrorType.BAD_REQUEST)
            is UpstreamServiceException -> error(env, ex.message ?: "Upstream service error", ErrorType.INTERNAL_ERROR)
            else -> null
        }

    private fun error(
        env: DataFetchingEnvironment,
        message: String,
        errorType: ErrorType,
    ): GraphQLError =
        GraphqlErrorBuilder
            .newError(env)
            .message(message)
            .errorType(errorType)
            .build()
}
