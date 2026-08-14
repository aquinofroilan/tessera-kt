package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.config.NotificationEmailProperties
import jakarta.mail.internet.InternetAddress
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Primary
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

/**
 * Hands a [jakarta.mail.internet.MimeMessage] to Spring's [JavaMailSender].
 * When the rendered [EmailContent] has an HTML body the message is sent as
 * multipart/alternative so receiving clients can pick the format they
 * support; otherwise plain-text only.
 *
 * Only loaded when a [JavaMailSender] bean exists — that's the Spring Boot
 * mail autoconfig signal that \`spring.mail.host\` (etc.) was actually
 * provided. Marked @Primary so when this bean is present it wins over
 * [NoOpEmailSender].
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
        content: EmailContent,
    ): Boolean {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, content.html != null, "UTF-8")
        helper.setFrom(buildFrom())
        helper.setTo(to)
        helper.setSubject(content.subject)
        if (content.html != null) {
            // The plain-text body is the multipart alternative; clients that
            // can't render HTML fall back to it.
            helper.setText(content.plainText, content.html)
        } else {
            helper.setText(content.plainText, false)
        }
        mailSender.send(message)
        log.debug("Sent notification email to {} (subject='{}', html={})", to, content.subject, content.html != null)
        return true
    }

    private fun buildFrom(): InternetAddress {
        val address = InternetAddress(properties.from)
        if (properties.senderName.isNotBlank()) address.personal = properties.senderName
        return address
    }
}
