package com.aquinofroilan.tessera.service.notification

import com.aquinofroilan.tessera.config.NotificationEmailProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Primary
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

/**
 * Hands a [SimpleMailMessage] to Spring's [JavaMailSender]. Only loaded when
 * a [JavaMailSender] bean exists — that's the Spring Boot mail autoconfig
 * signal that \`spring.mail.host\` (etc.) was actually provided.
 *
 * Marked @Primary so when this bean is present it wins over [NoOpEmailSender].
 */
@Component
@Primary
@ConditionalOnBean(JavaMailSender::class)
class SmtpEmailSender(
    private val mailSender: JavaMailSender,
    private val properties: NotificationEmailProperties,
) : EmailSender {
    private val log = LoggerFactory.getLogger(SmtpEmailSender::class.java)

    override fun send(
        to: String,
        subject: String,
        textBody: String,
    ): Boolean {
        val message =
            SimpleMailMessage().apply {
                from = formatFrom(properties.senderName, properties.from)
                setTo(to)
                this.subject = subject
                this.text = textBody
            }
        mailSender.send(message)
        log.debug("Sent notification email to {} (subject='{}')", to, subject)
        return true
    }

    private fun formatFrom(
        name: String,
        address: String,
    ): String = if (name.isBlank()) address else "$name <$address>"
}
