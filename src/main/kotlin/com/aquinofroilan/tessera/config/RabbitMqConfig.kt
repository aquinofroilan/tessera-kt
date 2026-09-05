package com.aquinofroilan.tessera.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfig {
    companion object {
        const val NOTIFICATION_EXCHANGE = "notification.exchange"
        const val EMAIL_QUEUE = "notification.email.queue"
        const val EMAIL_ROUTING_KEY = "notification.email"

        const val WEBHOOK_QUEUE = "notification.webhook.queue"
        const val WEBHOOK_ROUTING_KEY = "notification.webhook"

        const val DEAD_LETTER_EXCHANGE = "notification.dlx"
        const val DEAD_LETTER_QUEUE = "notification.dlq"

        const val DOMAIN_EVENT_EXCHANGE = "domain.event.exchange"
        const val DOMAIN_EVENT_WEBHOOK_QUEUE = "domain.event.webhook.queue"
        const val DOMAIN_EVENT_WEBHOOK_ROUTING_KEY = "domain.event.webhook"
    }

    @Bean
    fun jsonMessageConverter(): MessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun notificationExchange(): DirectExchange = DirectExchange(NOTIFICATION_EXCHANGE)

    @Bean
    fun deadLetterExchange(): DirectExchange = DirectExchange(DEAD_LETTER_EXCHANGE)

    @Bean
    fun deadLetterQueue(): Queue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build()

    @Bean
    fun deadLetterBinding(
        deadLetterQueue: Queue,
        deadLetterExchange: DirectExchange,
    ): Binding = BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_QUEUE)

    @Bean
    fun emailQueue(): Queue =
        QueueBuilder
            .durable(EMAIL_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
            .build()

    @Bean
    fun webhookQueue(): Queue =
        QueueBuilder
            .durable(WEBHOOK_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
            .build()

    @Bean
    fun emailBinding(
        emailQueue: Queue,
        notificationExchange: DirectExchange,
    ): Binding = BindingBuilder.bind(emailQueue).to(notificationExchange).with(EMAIL_ROUTING_KEY)

    @Bean
    fun webhookBinding(
        webhookQueue: Queue,
        notificationExchange: DirectExchange,
    ): Binding = BindingBuilder.bind(webhookQueue).to(notificationExchange).with(WEBHOOK_ROUTING_KEY)

    @Bean
    fun domainEventExchange(): org.springframework.amqp.core.TopicExchange =
        org.springframework.amqp.core
            .TopicExchange(DOMAIN_EVENT_EXCHANGE)

    @Bean
    fun domainEventWebhookQueue(): Queue =
        QueueBuilder
            .durable(DOMAIN_EVENT_WEBHOOK_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
            .build()

    @Bean
    fun domainEventWebhookBinding(
        domainEventWebhookQueue: Queue,
        notificationExchange: DirectExchange,
    ): Binding = BindingBuilder.bind(domainEventWebhookQueue).to(notificationExchange).with(DOMAIN_EVENT_WEBHOOK_ROUTING_KEY)
}
