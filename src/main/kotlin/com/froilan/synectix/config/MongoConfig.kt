package com.froilan.synectix.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration
import org.springframework.data.mongodb.config.EnableMongoAuditing

@Configuration
@EnableMongoAuditing
class MongoConfig : AbstractMongoClientConfiguration() {

    private val log = LoggerFactory.getLogger(MongoConfig::class.java)

    @Value("\${spring.data.mongodb.uri}")
    private lateinit var mongoUri: String

    override fun getDatabaseName(): String {
        val connectionString = ConnectionString(mongoUri)
        return connectionString.database ?: "synectix"
    }

    override fun mongoClient(): MongoClient {
        val connectionString = ConnectionString(mongoUri)
        val settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .build()
        log.debug("MongoDB connecting — database: {}, hosts: {}, credentials: {}", getDatabaseName(), connectionString.hosts, if (connectionString.credential != null) "configured" else "none")

        return MongoClients.create(settings)
    }
}
