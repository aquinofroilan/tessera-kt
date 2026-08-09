package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface NotificationRepository : JpaRepository<Notification, java.util.UUID> {
    fun findByRecipientUserIdAndOrganizationIdOrderByCreatedAtDesc(
        recipientUserId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<Notification>

    fun countByRecipientUserIdAndOrganizationIdAndReadAtIsNull(
        recipientUserId: java.util.UUID,
        organizationId: java.util.UUID,
    ): Long

    @Modifying
    @Query(
        """
        UPDATE Notification n
           SET n.readAt = :readAt
         WHERE n.recipientUserId = :recipientUserId
           AND n.organizationId = :organizationId
           AND n.readAt IS NULL
        """,
    )
    fun markAllReadFor(
        @Param("recipientUserId") recipientUserId: java.util.UUID,
        @Param("organizationId") organizationId: java.util.UUID,
        @Param("readAt") readAt: LocalDateTime,
    ): Int
}
