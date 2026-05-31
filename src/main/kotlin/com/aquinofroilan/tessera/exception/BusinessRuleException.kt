package com.aquinofroilan.tessera.exception

class BusinessRuleException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
