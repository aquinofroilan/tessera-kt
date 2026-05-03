package com.froilan.synectix.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("synectix.fx.auto-fetch")
data class FxAutoFetchProperties(
    val enabled: Boolean = true,
    val cron: String = "0 0 17 * * MON-FRI",
    val baseUrl: String = "https://api.frankfurter.dev/v1",
)
