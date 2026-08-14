package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.config.NotificationEmailProperties
import com.aquinofroilan.tessera.domain.notification.model.Notification
import org.springframework.stereotype.Component

/**
 * Renders a [Notification] into an [EmailContent] carrying both a plain-text
 * body (always) and an HTML body (when the channel is configured to send it).
 *
 * The HTML template is intentionally simple: inline styles in table cells,
 * brand palette baked in. Email clients can't be trusted with <style> tags
 * or modern CSS so the layout is table-based on purpose.
 */
@Component
class NotificationEmailRenderer(
    private val properties: NotificationEmailProperties,
) {
    fun render(notification: Notification): EmailContent =
        EmailContent(
            subject = notification.title,
            plainText = renderPlainText(notification),
            html = if (properties.html) renderHtml(notification) else null,
        )

    private fun renderPlainText(notification: Notification): String {
        val sb = StringBuilder()
        notification.body?.let { sb.append(it).append("\n\n") }
        notification.link?.let { sb.append("Open: ").append(it).append('\n') }
        if (sb.isEmpty()) sb.append(notification.title)
        return sb.toString().trimEnd()
    }

    private fun renderHtml(notification: Notification): String {
        val body = notification.body?.let { escape(it) }
        val link = notification.link?.let { escape(it) }
        val title = escape(notification.title)
        val senderName = escape(properties.senderName)

        return """
            <!doctype html>
            <html>
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>$title</title>
              </head>
              <body style="margin:0;padding:0;background:#f6f1e6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background:#f6f1e6;padding:32px 16px;">
                  <tr>
                    <td align="center">
                      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:560px;background:#ffffff;border-radius:8px;border:1px solid #e8e0d0;">
                        <tr>
                          <td style="padding:24px 32px 16px 32px;border-bottom:1px solid #e8e0d0;">
                            <div style="font-family:'Georgia',serif;font-size:14px;color:#b93a1d;letter-spacing:0.02em;">$senderName</div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:28px 32px 8px 32px;">
                            <h1 style="margin:0 0 12px 0;font-family:'Georgia',serif;font-size:22px;font-weight:380;color:#17160f;line-height:1.25;">$title</h1>
                          </td>
                        </tr>
                        ${if (body != null) """<tr><td style="padding:0 32px 24px 32px;font-size:15px;color:#3d3a30;line-height:1.55;">$body</td></tr>""" else ""}
                        ${if (link != null) """<tr><td style="padding:0 32px 28px 32px;"><a href="$link" style="display:inline-block;padding:10px 18px;background:#17160f;color:#f6f1e6;text-decoration:none;border-radius:999px;font-size:14px;">Open in Tessera</a></td></tr>""" else ""}
                        <tr>
                          <td style="padding:18px 32px 22px 32px;border-top:1px solid #e8e0d0;font-size:11px;color:#888477;letter-spacing:0.04em;text-transform:uppercase;">
                            You're getting this because you have notifications enabled for this kind of event. Update your preferences in Tessera.
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """.trimIndent()
    }

    private fun escape(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}
