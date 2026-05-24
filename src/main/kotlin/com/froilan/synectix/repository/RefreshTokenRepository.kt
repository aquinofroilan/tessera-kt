package com.froilan.synectix.repository

import com.froilan.synectix.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, String> {
    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>

    fun deleteByTokenHash(tokenHash: String)

    fun deleteBySessionTokenId(sessionTokenId: String)

    fun deleteBySessionTokenIdIn(sessionTokenIds: List<String>)

    fun deleteByUserId(userId: String)
}
