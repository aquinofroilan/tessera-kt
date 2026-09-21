package com.aquinofroilan.tessera.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun customOpenAPI(): OpenAPI {
        val securitySchemeName = "Bearer Auth"
        val apiKeySchemeName = "API Key"

        return OpenAPI()
            .info(
                Info()
                    .title("Tessera ERP API")
                    .version("1.8.0")
                    .description("REST API documentation for Tessera ERP System."),
            ).addSecurityItem(
                SecurityRequirement()
                    .addList(securitySchemeName)
                    .addList(apiKeySchemeName),
            ).components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT"),
                    ).addSecuritySchemes(
                        apiKeySchemeName,
                        SecurityScheme()
                            .name("X-API-Key")
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER),
                    ),
            )
    }
}
