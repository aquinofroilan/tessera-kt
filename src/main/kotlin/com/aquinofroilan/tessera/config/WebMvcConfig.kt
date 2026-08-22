package com.aquinofroilan.tessera.config

import com.aquinofroilan.tessera.security.CurrentOrganizationIdArgumentResolver
import com.aquinofroilan.tessera.security.CurrentUserIdArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val currentOrganizationIdArgumentResolver: CurrentOrganizationIdArgumentResolver,
    private val currentUserIdArgumentResolver: CurrentUserIdArgumentResolver,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentOrganizationIdArgumentResolver)
        resolvers.add(currentUserIdArgumentResolver)
    }
}
