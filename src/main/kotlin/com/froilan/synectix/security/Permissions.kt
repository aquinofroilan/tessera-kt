package com.froilan.synectix.security

object Permissions {
    const val SESSION_READ = "session:read"
    const val SESSION_DELETE = "session:delete"

    const val USER_READ = "user:read"
    const val USER_WRITE = "user:write"
    const val USER_DELETE = "user:delete"

    const val ORGANIZATION_READ = "organization:read"
    const val ORGANIZATION_WRITE = "organization:write"
    const val ORGANIZATION_DELETE = "organization:delete"

    const val ENVIRONMENT_READ = "environment:read"

    const val INVITATION_READ = "invitation:read"
    const val INVITATION_WRITE = "invitation:write"

    const val API_KEY_MANAGE = "apikey:manage"

    val ALL_PERMISSIONS =
        listOf(
            SESSION_READ,
            SESSION_DELETE,
            USER_READ,
            USER_WRITE,
            USER_DELETE,
            ORGANIZATION_READ,
            ORGANIZATION_WRITE,
            ORGANIZATION_DELETE,
            ENVIRONMENT_READ,
            INVITATION_READ,
            INVITATION_WRITE,
            API_KEY_MANAGE,
        )
}
