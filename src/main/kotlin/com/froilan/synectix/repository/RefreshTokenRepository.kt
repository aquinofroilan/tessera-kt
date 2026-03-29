package com.froilan.synectix.repository

import com.froilan.synectix.model.RefreshToken
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RefreshTokenRepository : MongoRepository<RefreshToken, String> {
    fun findByToken(token: String): Optional<RefreshToken>
    fun deleteByToken(token: String)
    fun deleteBySessionTokenId(sessionTokenId: String)
    fun deleteByUserId(userId: String)
}
