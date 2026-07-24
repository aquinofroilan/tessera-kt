package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.config.PostHogProperties
import com.posthog.java.PostHog
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.support.DefaultListableBeanFactory

class PostHogLoggingServiceTest {
    @Test
    fun `captureException should send metadata-only event when enabled`() {
        val postHog = mock<PostHog>()
        val beanFactory = DefaultListableBeanFactory()
        beanFactory.registerSingleton("postHog", postHog)

        val service =
            PostHogLoggingService(
                postHogProperties = PostHogProperties(enabled = true, loggingEnabled = true, distinctId = "50a05a28-c428-37f5-852e-9a226ac073f6"),
                postHogProvider = beanFactory.getBeanProvider(PostHog::class.java),
            )

        service.captureException(level = "ERROR", source = "service", throwable = RuntimeException("boom: ssn 123-45-6789"))

        val propsCaptor = argumentCaptor<Map<String, Any>>()
        verify(postHog).capture(eq("test-app"), eq("application_log"), propsCaptor.capture())
        val props = propsCaptor.firstValue
        assertThat(props).containsKeys("level", "source", "exception_type", "exception_origin")
        assertThat(props["exception_type"]).isEqualTo("java.lang.RuntimeException")
        // Raw message text must never be forwarded to the third party.
        assertThat(props).doesNotContainKeys("message", "exception_message")
        assertThat(props.values.map { it.toString() }).noneMatch { it.contains("123-45-6789") }
    }

    @Test
    fun `captureException must not throw when the PostHog SDK fails`() {
        val postHog = mock<PostHog>()
        whenever(postHog.capture(any(), any(), any<Map<String, Any>>()))
            .thenThrow(RuntimeException("SDK down"))
        val beanFactory = DefaultListableBeanFactory()
        beanFactory.registerSingleton("postHog", postHog)

        val service =
            PostHogLoggingService(
                postHogProperties = PostHogProperties(enabled = true, loggingEnabled = true, distinctId = "50a05a28-c428-37f5-852e-9a226ac073f6"),
                postHogProvider = beanFactory.getBeanProvider(PostHog::class.java),
            )

        assertThatCode {
            service.captureException(level = "ERROR", source = "service", throwable = RuntimeException("boom"))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `captureException should no-op when disabled`() {
        val postHog = mock<PostHog>()
        val beanFactory = DefaultListableBeanFactory()
        beanFactory.registerSingleton("postHog", postHog)

        val service =
            PostHogLoggingService(
                postHogProperties = PostHogProperties(enabled = false, loggingEnabled = true, distinctId = "50a05a28-c428-37f5-852e-9a226ac073f6"),
                postHogProvider = beanFactory.getBeanProvider(PostHog::class.java),
            )

        assertThatCode {
            service.captureException(level = "INFO", source = "service", throwable = RuntimeException("ok"))
        }.doesNotThrowAnyException()
        verify(postHog, never()).capture(any(), any(), any<Map<String, Any>>())
    }
}
