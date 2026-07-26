package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, java.util.UUID> {
    fun findByTokenHash(tokenHash: String): Optional<PasswordResetToken>

    fun deleteByUserId(userId: java.util.UUID)
}
