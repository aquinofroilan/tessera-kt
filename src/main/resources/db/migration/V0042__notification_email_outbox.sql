-- Email delivery outbox for notifications. The publish path enqueues a row
-- here per notification (when email is enabled and the recipient has an
-- address); a scheduled dispatcher (NotificationEmailDispatcher) picks up
-- PENDING rows, attempts SMTP delivery, and either marks them SENT or
-- schedules a retry up to a configured cap.
--
-- recipient_email is snapshotted at enqueue time so a later address change
-- doesn't silently retarget a queued message.
CREATE TABLE notification_email_outbox (
    id                uuid PRIMARY KEY,
    notification_id   uuid NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    recipient_email   text NOT NULL,
    status            text NOT NULL DEFAULT 'PENDING',
    attempts          integer NOT NULL DEFAULT 0,
    last_error        text,
    scheduled_at      timestamp NOT NULL DEFAULT current_timestamp,
    sent_at           timestamp,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT notification_email_outbox_status_chk
        CHECK (status IN ('PENDING','SENT','FAILED','SKIPPED'))
);
CREATE INDEX notification_email_outbox_dispatch_idx
    ON notification_email_outbox(status, scheduled_at)
    WHERE status = 'PENDING';
CREATE INDEX notification_email_outbox_notification_idx
    ON notification_email_outbox(notification_id);
