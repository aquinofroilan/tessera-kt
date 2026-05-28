package com.loom.synectix.exception

class BusinessRuleException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
