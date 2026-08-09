package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.EmailDeliveryStatus
import com.aquinofroilan.tessera.model.NotificationEmailOutbox
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface NotificationEmailOutboxRepository : JpaRepository<NotificationEmailOutbox, java.util.UUID> {
    fun findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
        status: EmailDeliveryStatus,
        scheduledAt: LocalDateTime,
        limit: Limit,
    ): List<NotificationEmailOutbox>

    fun countByStatus(status: EmailDeliveryStatus): Long
}
