package com.aquinofroilan.tessera.service.notification

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

/**
 * Fallback [EmailSender] for environments without SMTP configured (local
 * dev, CI without mail server, tests). Returns false so [NotificationEmailDispatcher]
 * marks the outbox row SKIPPED rather than retrying forever — the row stays
 * for audit / debug, but the dispatcher won't keep picking it up.
 */
@Component
@ConditionalOnMissingBean(JavaMailSender::class)
class NoOpEmailSender : EmailSender {
    private val log = LoggerFactory.getLogger(NoOpEmailSender::class.java)

    override fun send(
        to: String,
        subject: String,
        textBody: String,
    ): Boolean {
        log.info(
            "Skipping notification email to {} (subject='{}') — no JavaMailSender bean. " +
                "Configure spring.mail.* to enable SMTP delivery.",
            to,
            subject,
        )
        return false
    }
}
