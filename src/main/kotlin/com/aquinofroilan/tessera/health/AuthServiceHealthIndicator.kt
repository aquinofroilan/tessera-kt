package com.aquinofroilan.tessera.health

import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component
class AuthServiceHealthIndicator(
    private val userRepository: UserRepository,
) : HealthIndicator {
    override fun health(): Health =
        try {
            val userCount = userRepository.count()

            Health
                .up()
                .withDetail("service", "Authentication Service")
                .withDetail("userCount", userCount)
                .withDetail("status", "Operational")
                .build()
        } catch (e: Exception) {
            Health
                .down()
                .withDetail("service", "Authentication Service")
                .withDetail("error", e.message ?: "Authentication service error")
                .withDetail("status", "Failed")
                .withException(e)
                .build()
        }
}
