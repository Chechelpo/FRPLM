package chechelpo.demo.events;

import chechelpo.demo.events.types.Event;

import java.util.*;

public final class EventManager {
    private static final Set<EventListener> listeners = new HashSet<>();

    public static void subscribe(EventListener eventListener) {
        listeners.add(eventListener);
    }

    public static void emitEvent(Event event) {
        for (EventListener eventListener : listeners) {
            eventListener.onEvent(event);
        }
    }
}
