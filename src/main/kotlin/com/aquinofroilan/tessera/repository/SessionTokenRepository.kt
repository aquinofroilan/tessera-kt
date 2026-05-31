package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.SessionToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface SessionTokenRepository : JpaRepository<SessionToken, String> {
    fun findByToken(token: String): Optional<SessionToken>

    fun findByUserId(userId: String): List<SessionToken>

    fun findByUserIdAndExpiryAtAfter(
        userId: String,
        expiryAt: LocalDateTime,
    ): List<SessionToken>

    fun deleteByToken(token: String)

    fun deleteByUserId(userId: String)

    fun findByUserIdAndTokenNot(
        userId: String,
        token: String,
    ): List<SessionToken>
}
