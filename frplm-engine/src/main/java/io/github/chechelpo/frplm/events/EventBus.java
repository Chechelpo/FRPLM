package io.github.chechelpo.frplm.events;

import io.github.chechelpo.frplm.events.crud.CRUDEvent;

public interface EventBus {
    long nextOperationID();
    void publish(CRUDEvent event);
}
