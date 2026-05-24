package com.froilan.synectix.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

@Configuration
@EnableMongoAuditing
class MongoConfig : AbstractMongoClientConfiguration() {
    private val log = LoggerFactory.getLogger(MongoConfig::class.java)

    @Value("\${spring.data.mongodb.uri}")
    private lateinit var mongoUri: String

    override fun autoIndexCreation(): Boolean = true

    override fun getDatabaseName(): String {
        val connectionString = ConnectionString(mongoUri)
        return connectionString.database ?: "synectix"
    }

    @Bean
    fun transactionManager(dbFactory: MongoDatabaseFactory): MongoTransactionManager = MongoTransactionManager(dbFactory)

    override fun mongoClient(): MongoClient {
        val connectionString = ConnectionString(mongoUri)
        val settings =
            MongoClientSettings
                .builder()
                .applyConnectionString(connectionString)
                .build()
        log.debug(
            "MongoDB connecting — database: {}, hosts: {}, credentials: {}",
            getDatabaseName(),
            connectionString.hosts,
            if (connectionString.credential !=
                null
            ) {
                "configured"
            } else {
                "none"
            },
        )

        return MongoClients.create(settings)
    }
}

@Component
@Order(0)
class MongoIndexInitializer(
    private val mongoTemplate: MongoTemplate,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(MongoIndexInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        val collection = mongoTemplate.getCollection("invitations")
        collection.createIndex(
            Indexes.compoundIndex(
                Document("email", 1),
                Document("organizationId", 1),
            ),
            IndexOptions()
                .unique(true)
                .partialFilterExpression(Filters.eq("status", "PENDING"))
                .name("unique_pending_invitation_per_email_org"),
        )
        log.info("Ensured partial unique index on invitations(email, organizationId) where status=PENDING")
    }
}

@Component
@Order(1)
class StockOnHandBackfillRunner(
    private val mongoTemplate: MongoTemplate,
    private val stockMovementQueries: com.froilan.synectix.repository.StockMovementQueries,
    private val stockOnHandRepository: com.froilan.synectix.repository.StockOnHandRepository,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(StockOnHandBackfillRunner::class.java)

    override fun run(args: ApplicationArguments) {
        val counterCount = mongoTemplate.getCollection("stock_on_hand").countDocuments()
        if (counterCount > 0L) return

        val movementCount = mongoTemplate.getCollection("stock_movements").countDocuments()
        if (movementCount == 0L) return

        log.info("Backfilling stock_on_hand from {} stock_movements...", movementCount)

        val orgIds =
            mongoTemplate
                .getCollection("stock_movements")
                .distinct("organizationId", String::class.java)
                .into(mutableListOf())

        var inserted = 0
        for (orgId in orgIds) {
            val totals = stockMovementQueries.onHandByProductWarehouse(orgId)
            for ((key, qty) in totals) {
                stockOnHandRepository.applyDelta(
                    organizationId = orgId,
                    productId = key.productId,
                    warehouseId = key.warehouseId,
                    delta = qty,
                    allowNegative = true,
                )
                inserted++
            }
        }
        log.info("stock_on_hand backfill complete: {} counter rows seeded", inserted)
    }
}
