package com.aquinofroilan.tessera.security

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

    const val AR_CREATE = "ar:create"
    const val AR_READ = "ar:read"
    const val AR_APPROVE = "ar:approve"
    const val AR_RECEIVE = "ar:receive"
    const val AR_VOID = "ar:void"

    const val TAX_CREATE = "tax:create"
    const val TAX_READ = "tax:read"
    const val TAX_DELETE = "tax:delete"

    const val FX_READ = "fx:read"
    const val FX_CREATE = "fx:create"

    const val INVENTORY_READ = "inventory:read"
    const val INVENTORY_WRITE = "inventory:write"

    const val PROCUREMENT_READ = "procurement:read"
    const val PROCUREMENT_WRITE = "procurement:write"
    const val PROCUREMENT_APPROVE = "procurement:approve"
    const val PROCUREMENT_RECEIVE = "procurement:receive"

    const val SALES_READ = "sales:read"
    const val SALES_WRITE = "sales:write"
    const val SALES_APPROVE = "sales:approve"
    const val SALES_FULFILL = "sales:fulfill"

    const val HR_READ = "hr:read"
    const val HR_WRITE = "hr:write"
    const val HR_APPROVE = "hr:approve"

    const val PROJECT_READ = "projects:read"
    const val PROJECT_WRITE = "projects:write"
    const val PROJECT_APPROVE = "projects:approve"

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
            AR_CREATE,
            AR_READ,
            AR_APPROVE,
            AR_RECEIVE,
            AR_VOID,
            TAX_CREATE,
            TAX_READ,
            TAX_DELETE,
            FX_READ,
            FX_CREATE,
            INVENTORY_READ,
            INVENTORY_WRITE,
            PROCUREMENT_READ,
            PROCUREMENT_WRITE,
            PROCUREMENT_APPROVE,
            PROCUREMENT_RECEIVE,
            SALES_READ,
            SALES_WRITE,
            SALES_APPROVE,
            SALES_FULFILL,
            HR_READ,
            HR_WRITE,
            HR_APPROVE,
            PROJECT_READ,
            PROJECT_WRITE,
            PROJECT_APPROVE,
        )
}
