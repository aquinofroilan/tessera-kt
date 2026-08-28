package com.aquinofroilan.tessera.domain.auth.repository

import com.aquinofroilan.tessera.domain.auth.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, java.util.UUID> {
    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>

    fun deleteByTokenHash(tokenHash: String)

    fun deleteBySessionTokenId(sessionTokenId: java.util.UUID)

    fun deleteBySessionTokenIdIn(sessionTokenIds: List<java.util.UUID>)

    fun deleteByUserId(userId: java.util.UUID)
}
