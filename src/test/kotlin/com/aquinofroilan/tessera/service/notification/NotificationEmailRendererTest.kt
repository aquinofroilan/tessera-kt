package com.aquinofroilan.tessera.service.notification

import com.aquinofroilan.tessera.config.NotificationEmailProperties
import com.aquinofroilan.tessera.model.Notification
import com.aquinofroilan.tessera.model.NotificationCategory
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

        assertThat(content.html).isNotNull()
        assertThat(content.html!!).contains("Your leave was approved")
        assertThat(content.html!!).contains("/hr/leave-requests/lr-1")
        assertThat(content.html!!).contains("Open in Tessera")
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

        assertThat(content.html!!).doesNotContain("<script>evil</script>")
        assertThat(content.html!!).contains("&lt;script&gt;evil&lt;/script&gt;")
        assertThat(content.html!!).contains("a &amp; b &lt; c")
        assertThat(content.html!!).contains("https://example.com/?q=&quot;x&quot;")
    }

    private fun notification(
        title: String,
        body: String? = null,
        link: String? = null,
    ): Notification =
        Notification(
            organizationId = "org-1",
            recipientUserId = "user-1",
            category = NotificationCategory.APPROVAL,
            kind = "test.kind",
            title = title,
            body = body,
            link = link,
        )
}
