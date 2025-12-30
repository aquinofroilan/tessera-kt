package com.froilan.synectix.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration
import org.springframework.data.mongodb.config.EnableMongoAuditing

@Configuration
@EnableMongoAuditing
class MongoConfig : AbstractMongoClientConfiguration() {

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
        println("MongoDB Configuration:")
        println("  Database: ${getDatabaseName()}")
        println("  Hosts: ${connectionString.hosts}")
        println("  Credential: ${if (connectionString.credential != null) "***configured***" else "null"}")

        return MongoClients.create(settings)
    }
}
