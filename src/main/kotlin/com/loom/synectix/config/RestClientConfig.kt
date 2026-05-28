package com.loom.synectix.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class RestClientConfig {
    @Bean
    fun restClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(Duration.ofSeconds(5).toMillis().toInt())
        factory.setReadTimeout(Duration.ofSeconds(10).toMillis().toInt())
        return RestClient.builder().requestFactory(factory).build()
    }
}
