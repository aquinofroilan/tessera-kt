package com.froilan.synectix.util

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.HexFormat

@Component
class TokenHasher(
    @Value("\${security.token.hash-algorithm:SHA-256}")
    private val algorithm: String,
) {
    @PostConstruct
    fun validate() {
        try {
            MessageDigest.getInstance(algorithm)
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException(
                "Invalid token hash algorithm '$algorithm'. " +
                    "Check security.token.hash-algorithm property.",
                e,
            )
        }
    }

    fun hash(token: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
        return HexFormat.of().formatHex(hashBytes)
    }
}
