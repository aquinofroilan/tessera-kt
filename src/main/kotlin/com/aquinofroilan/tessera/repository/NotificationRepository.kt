package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface NotificationRepository : JpaRepository<Notification, String> {
    fun findByRecipientUserIdAndOrganizationIdOrderByCreatedAtDesc(
        recipientUserId: String,
        organizationId: String,
    ): List<Notification>

    fun countByRecipientUserIdAndOrganizationIdAndReadAtIsNull(
        recipientUserId: String,
        organizationId: String,
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
        @Param("recipientUserId") recipientUserId: String,
        @Param("organizationId") organizationId: String,
        @Param("readAt") readAt: LocalDateTime,
    ): Int
}
