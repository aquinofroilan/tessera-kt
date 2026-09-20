package com.aquinofroilan.tessera.config

import com.aquinofroilan.tessera.config.export.CsvHttpMessageConverter
import com.aquinofroilan.tessera.config.export.ExcelHttpMessageConverter
import com.aquinofroilan.tessera.config.export.PdfHttpMessageConverter
import com.aquinofroilan.tessera.security.CurrentOrganizationIdArgumentResolver
import com.aquinofroilan.tessera.security.CurrentUserIdArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val currentOrganizationIdArgumentResolver: CurrentOrganizationIdArgumentResolver,
    private val currentUserIdArgumentResolver: CurrentUserIdArgumentResolver,
    private val organizationStatusInterceptor: OrganizationStatusInterceptor,
    private val csvHttpMessageConverter: CsvHttpMessageConverter,
    private val excelHttpMessageConverter: ExcelHttpMessageConverter,
    private val pdfHttpMessageConverter: PdfHttpMessageConverter,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentOrganizationIdArgumentResolver)
        resolvers.add(currentUserIdArgumentResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(organizationStatusInterceptor)
    }

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer
            .favorParameter(true)
            .parameterName("format")
            .ignoreAcceptHeader(false)
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("csv", MediaType("text", "csv"))
            .mediaType("xlsx", MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .mediaType("pdf", MediaType("application", "pdf"))
    }

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.add(csvHttpMessageConverter)
        converters.add(excelHttpMessageConverter)
        converters.add(pdfHttpMessageConverter)
    }
}
