package com.froilan.synectix.repository

import com.froilan.synectix.model.SessionToken
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SessionTokenRepository : MongoRepository<SessionToken, String> {
    fun findByToken(token: String): Optional<SessionToken>

    fun deleteByToken(token: String)
}
