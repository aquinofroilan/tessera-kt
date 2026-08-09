package com.aquinofroilan.tessera.service.notification

/**
 * Wire shape an [EmailSender] hands off to its transport. Carries both the
 * plain-text body (required) and an optional HTML body — when present the
 * SMTP sender ships a multipart/alternative message and the receiving
 * client picks whichever it supports.
 */
data class EmailContent(
    val subject: String,
    val plainText: String,
    val html: String? = null,
)
