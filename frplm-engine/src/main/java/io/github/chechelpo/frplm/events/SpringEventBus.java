package io.github.chechelpo.frplm.events;

import io.github.chechelpo.frplm.events.crud.CRUDEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public final class SpringEventBus implements EventBus {
    private final ApplicationEventPublisher publisher;
    private final AtomicLong operationIds = new AtomicLong();

    public SpringEventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public long nextOperationID() {
        return operationIds.getAndIncrement();
    }

    @Override
    public void publish(@NotNull CRUDEvent event) {
        publisher.publishEvent(event);
    }
}