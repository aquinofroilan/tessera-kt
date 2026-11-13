package com.aquinofroilan.tessera.exception

class ConflictException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
