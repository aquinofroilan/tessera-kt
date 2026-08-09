package com.aquinofroilan.tessera.service.notification

/**
 * Well-known notification kind strings. The DB column is freeform so any
 * service can mint a new value, but keeping the ones we publish ourselves
 * in one place avoids drift between the listener that writes them and
 * the preference UI that lets users opt out.
 */
object NotificationKinds {
    const val LEAVE_REQUEST_APPROVED = "leave_request.approved"
    const val LEAVE_REQUEST_REJECTED = "leave_request.rejected"
    const val PURCHASE_REQUEST_APPROVED = "purchase_request.approved"
    const val PURCHASE_REQUEST_REJECTED = "purchase_request.rejected"
}
