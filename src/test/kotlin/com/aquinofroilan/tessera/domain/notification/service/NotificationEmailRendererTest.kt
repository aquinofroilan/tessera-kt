package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.config.NotificationEmailProperties
import com.aquinofroilan.tessera.domain.notification.model.Notification
import com.aquinofroilan.tessera.domain.notification.model.NotificationCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NotificationEmailRendererTest {
    @Test
    fun `subject mirrors the notification title`() {
        val renderer = NotificationEmailRenderer(NotificationEmailProperties())

        val content = renderer.render(notification(title = "Your leave request was approved"))

        assertThat(content.subject).isEqualTo("Your leave request was approved")
    }

    @Test
    fun `plain text body composes body and link with a fallback to the title`() {
        val renderer = NotificationEmailRenderer(NotificationEmailProperties())

        val rich =
            renderer.render(
                notification(
                    title = "Purchase request PR-001 approved",
                    body = "Ready to convert into a PO.",
                    link = "/procurement/purchase-requests/pr-1",
                ),
            )
        assertThat(rich.plainText).contains("Ready to convert into a PO.")
        assertThat(rich.plainText).contains("Open: /procurement/purchase-requests/pr-1")

        val bare = renderer.render(notification(title = "Heads up", body = null, link = null))
        assertThat(bare.plainText).isEqualTo("Heads up")
    }

    @Test
    fun `html body is emitted when enabled and includes the title and CTA`() {
        val renderer = NotificationEmailRenderer(NotificationEmailProperties())

        val content =
            renderer.render(
                notification(
                    title = "Your leave was approved",
                    body = "Aug 1 - 3.",
                    link = "/hr/leave-requests/lr-1",
                ),
            )

        val html = content.html
        assertThat(html).isNotNull
        assertThat(html).contains("Your leave was approved")
        assertThat(html).contains("/hr/leave-requests/lr-1")
        assertThat(html).contains("Open in Tessera")
    }

    @Test
    fun `html body is omitted when the channel is configured plain-text only`() {
        val renderer = NotificationEmailRenderer(NotificationEmailProperties(html = false))

        val content = renderer.render(notification(title = "Plain only"))

        assertThat(content.html).isNull()
    }

    @Test
    fun `html escapes user-supplied content to prevent injection`() {
        val renderer = NotificationEmailRenderer(NotificationEmailProperties())

        val content =
            renderer.render(
                notification(
                    title = "<script>evil</script>",
                    body = "a & b < c",
                    link = "https://example.com/?q=\"x\"",
                ),
            )

        val html = content.html
        assertThat(html).isNotNull
        assertThat(html).doesNotContain("<script>evil</script>")
        assertThat(html).contains("&lt;script&gt;evil&lt;/script&gt;")
        assertThat(html).contains("a &amp; b &lt; c")
        assertThat(html).contains("https://example.com/?q=&quot;x&quot;")
    }

    private fun notification(
        title: String,
        body: String? = null,
        link: String? = null,
    ): Notification =
        Notification(
            organizationId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000100"),
            recipientUserId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000101"),
            category = NotificationCategory.APPROVAL,
            kind = "test.kind",
            title = title,
            body = body,
            link = link,
        )
}
