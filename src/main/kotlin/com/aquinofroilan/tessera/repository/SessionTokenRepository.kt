package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.SessionToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface SessionTokenRepository : JpaRepository<SessionToken, java.util.UUID> {
    fun findByToken(token: String): Optional<SessionToken>

    fun findByUserId(userId: java.util.UUID): List<SessionToken>

    fun findByUserIdAndExpiryAtAfter(
        userId: java.util.UUID,
        expiryAt: LocalDateTime,
    ): List<SessionToken>

    fun deleteByToken(token: String)

    fun deleteByUserId(userId: java.util.UUID)

    fun findByUserIdAndTokenNot(
        userId: java.util.UUID,
        token: String,
    ): List<SessionToken>
}
