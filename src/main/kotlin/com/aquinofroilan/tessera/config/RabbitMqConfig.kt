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
    }

    @Bean
    fun jsonMessageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun notificationExchange(): DirectExchange {
        return DirectExchange(NOTIFICATION_EXCHANGE)
    }

    @Bean
    fun deadLetterExchange(): DirectExchange {
        return DirectExchange(DEAD_LETTER_EXCHANGE)
    }

    @Bean
    fun deadLetterQueue(): Queue {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build()
    }

    @Bean
    fun emailQueue(): Queue {
        return QueueBuilder.durable(EMAIL_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
            .build()
    }

    @Bean
    fun webhookQueue(): Queue {
        return QueueBuilder.durable(WEBHOOK_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
            .build()
    }

    @Bean
    fun emailBinding(emailQueue: Queue, notificationExchange: DirectExchange): Binding {
        return BindingBuilder.bind(emailQueue).to(notificationExchange).with(EMAIL_ROUTING_KEY)
    }

    @Bean
    fun webhookBinding(webhookQueue: Queue, notificationExchange: DirectExchange): Binding {
        return BindingBuilder.bind(webhookQueue).to(notificationExchange).with(WEBHOOK_ROUTING_KEY)
    }
}
