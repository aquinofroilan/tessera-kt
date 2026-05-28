package com.loom.synectix.security

import com.github.benmanes.caffeine.cache.Caffeine
import com.loom.synectix.repository.RoleRepository
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

    fun getPermissions(roleName: String): Set<String> = cache.get(roleName)

    fun refresh() {
        cache.invalidateAll()
        loadAll()
    }
}
