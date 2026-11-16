package com.aquinofroilan.tessera.exception

class UnprocessableRequestException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
