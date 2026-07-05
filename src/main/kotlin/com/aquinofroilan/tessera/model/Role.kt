package com.aquinofroilan.tessera.model

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

enum class RoleLevel {
    SYSTEM,
    ORGANIZATION,
}

@Entity
@Table(name = "roles")
@EntityListeners(AuditingEntityListener::class)
class Role(
    @Id
    @Column(name = "uuid", columnDefinition = "uuid")
    var uuid: String = UUID.randomUUID().toString(),
    var name: String,
    var description: String,
    @Enumerated(EnumType.STRING)
    var level: RoleLevel,
    @Column(name = "is_default")
    var isDefault: Boolean = false,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "role_permissions",
        joinColumns = [JoinColumn(name = "role_id")],
    )
    @Column(name = "permission")
    var permissions: List<String> = emptyList(),
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
