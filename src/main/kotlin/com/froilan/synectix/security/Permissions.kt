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

    const val ACCOUNT_CREATE = "account:create"
    const val ACCOUNT_READ = "account:read"
    const val ACCOUNT_UPDATE = "account:update"
    const val ACCOUNT_DELETE = "account:delete"

    const val JOURNAL_CREATE = "journal:create"
    const val JOURNAL_READ = "journal:read"
    const val JOURNAL_POST = "journal:post"
    const val JOURNAL_VOID = "journal:void"

    const val FISCAL_CREATE = "fiscal:create"
    const val FISCAL_READ = "fiscal:read"
    const val FISCAL_CLOSE = "fiscal:close"

    const val AP_CREATE = "ap:create"
    const val AP_READ = "ap:read"
    const val AP_APPROVE = "ap:approve"
    const val AP_PAY = "ap:pay"
    const val AP_VOID = "ap:void"

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
            ACCOUNT_CREATE,
            ACCOUNT_READ,
            ACCOUNT_UPDATE,
            ACCOUNT_DELETE,
            JOURNAL_CREATE,
            JOURNAL_READ,
            JOURNAL_POST,
            JOURNAL_VOID,
            FISCAL_CREATE,
            FISCAL_READ,
            FISCAL_CLOSE,
            AP_CREATE,
            AP_READ,
            AP_APPROVE,
            AP_PAY,
            AP_VOID,
        )
}
