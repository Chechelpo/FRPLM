package chechelpo.frplm.events;

import chechelpo.frplm.events.crud.CRUDEvent;

public interface EventBus {
    long nextOperationID();
    void publish(CRUDEvent event);
}
