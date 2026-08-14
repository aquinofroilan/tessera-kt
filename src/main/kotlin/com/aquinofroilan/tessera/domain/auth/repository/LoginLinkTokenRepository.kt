package com.aquinofroilan.tessera.domain.auth.repository

import com.aquinofroilan.tessera.domain.auth.model.LoginLinkToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface LoginLinkTokenRepository : JpaRepository<LoginLinkToken, java.util.UUID> {
    fun findByTokenHash(tokenHash: String): Optional<LoginLinkToken>

    fun deleteByUserId(userId: java.util.UUID): Int
}
