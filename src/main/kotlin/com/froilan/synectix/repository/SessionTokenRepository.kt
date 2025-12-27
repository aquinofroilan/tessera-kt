package com.froilan.synectix.repository

import com.froilan.synectix.model.SessionToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SessionTokenRepository : JpaRepository<SessionToken, Long> {
    fun findByToken(token: String): Optional<SessionToken>
    fun deleteByToken(token: String)
}
