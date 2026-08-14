package com.aquinofroilan.tessera.security

import com.aquinofroilan.tessera.domain.auth.repository.RoleRepository
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RolePermissionCache(
    private val roleRepository: RoleRepository,
) {
    private val cache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build<String, Set<String>> { name ->
                roleRepository.findByName(name).map { it.permissions.toSet() }.orElse(emptySet())
            }

    @PostConstruct
    fun loadAll() {
        roleRepository.findAll().forEach { role ->
            cache.put(role.name, role.permissions.toSet())
        }
    }

    fun getPermissions(roleName: String): Set<String> {
        val cached = cache.getIfPresent(roleName)
        if (cached != null) {
            return cached
        }
        val permissions = roleRepository.findByName(roleName).map { it.permissions.toSet() }.orElse(emptySet())
        cache.put(roleName, permissions)
        return permissions
    }

    fun refresh() {
        cache.invalidateAll()
        loadAll()
    }
}
