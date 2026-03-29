package com.froilan.synectix.repository

import com.froilan.synectix.model.PasswordResetToken
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PasswordResetTokenRepository : MongoRepository<PasswordResetToken, String> {
    fun findByTokenHash(tokenHash: String): Optional<PasswordResetToken>

    fun deleteByUserId(userId: String)
}
