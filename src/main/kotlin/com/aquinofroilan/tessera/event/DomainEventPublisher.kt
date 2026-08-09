package com.aquinofroilan.tessera.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Thin wrapper over Spring's [ApplicationEventPublisher] that exists for
 * two reasons:
 *
 * 1. It lets domain services depend on a Tessera-owned type instead of a
 *    Spring framework type — cheap, but keeps grep-for-callers tidy.
 * 2. It restricts what can be published to [DomainEvent] subtypes, so a
 *    stray \`publisher.publish("hello")\` won't compile.
 *
 * Listeners use \`@TransactionalEventListener(AFTER_COMMIT)\` so events fire
 * after the source transaction commits successfully.
 */
@Component
class DomainEventPublisher(
    private val publisher: ApplicationEventPublisher,
) {
    fun publish(event: DomainEvent) {
        publisher.publishEvent(event)
    }
}
