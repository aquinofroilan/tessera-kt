package com.aquinofroilan.tessera.domain.auth.dto

data class RegisterOAuth2ClientRequest(
    val name: String,
    val redirectUris: List<String>,
    val allowedScopes: List<String>,
)

data class RegisterOAuth2ClientResponse(
    val clientId: String,
    val clientSecret: String,
    val name: String,
)

data class AuthorizeOAuth2Request(
    val clientId: String,
    val redirectUri: String,
    val scopes: List<String>,
)

data class AuthorizeOAuth2Response(
    val code: String,
    val redirectUri: String,
)

data class TokenExchangeRequest(
    val grantType: String,
    val code: String,
    val redirectUri: String,
    val clientId: String,
    val clientSecret: String,
)

data class TokenExchangeResponse(
    val access_token: String,
    val token_type: String = "Bearer",
    val expires_in: Long,
)
