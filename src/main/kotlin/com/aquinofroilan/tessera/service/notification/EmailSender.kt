package com.aquinofroilan.tessera.service.notification

/**
 * Abstraction over outbound email so the dispatcher doesn't depend on a
 * specific transport. [SmtpEmailSender] uses Spring's JavaMailSender when
 * configured; [NoOpEmailSender] is the fallback the app loads in dev /
 * test when no SMTP credentials are present, so [NotificationEmailDispatcher]
 * still resolves a bean and marks outbox rows SKIPPED rather than retrying
 * forever.
 */
interface EmailSender {
    /**
     * @return true if the message was handed off to the transport,
     *         false if intentionally dropped (no-op / unconfigured).
     *         Throw for transport-level failures so the dispatcher can
     *         retry with backoff.
     */
    fun send(
        to: String,
        subject: String,
        textBody: String,
    ): Boolean
}
