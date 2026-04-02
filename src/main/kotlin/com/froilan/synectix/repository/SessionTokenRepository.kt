package com.froilan.synectix.repository

import com.froilan.synectix.model.SessionToken
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface SessionTokenRepository : MongoRepository<SessionToken, String> {
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
