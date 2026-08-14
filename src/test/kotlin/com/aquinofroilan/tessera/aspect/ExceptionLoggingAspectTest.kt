package com.aquinofroilan.tessera.aspect

import com.aquinofroilan.tessera.domain.platform.service.PostHogLoggingService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ExceptionLoggingAspectTest {
    private val postHogLoggingService = mock<PostHogLoggingService>()
    private val aspect = ExceptionLoggingAspect(postHogLoggingService)

    @Test
    fun `logServiceException should forward to PostHog logging service`() {
        val exception = IllegalStateException("service error")

        aspect.logServiceException(exception)

        verify(postHogLoggingService).captureException(
            level = eq("ERROR"),
            source = eq("service"),
            throwable = eq(exception),
        )
    }

    @Test
    fun `logControllerException should forward to PostHog logging service`() {
        val exception = IllegalArgumentException("controller error")

        aspect.logControllerException(exception)

        verify(postHogLoggingService).captureException(
            level = eq("ERROR"),
            source = eq("controller"),
            throwable = eq(exception),
        )
    }

    @Test
    fun `logRepositoryException should forward to PostHog logging service`() {
        val exception = RuntimeException("repository error")

        aspect.logRepositoryException(exception)

        verify(postHogLoggingService).captureException(
            level = eq("ERROR"),
            source = eq("repository"),
            throwable = eq(exception),
        )
    }
}
