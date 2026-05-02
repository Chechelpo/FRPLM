package chechelpo.frplm.events;

import ch.qos.logback.classic.Logger;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import org.jetbrains.annotations.NotNull;
import org.jooq.Record;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class EventManager {
    private EventManager() {}

    private static final Logger log = (Logger) LoggerFactory.getLogger(EventManager.class.getSimpleName());

    private static final Set<EventListener> listeners = new HashSet<>();
    private static final Set<Event> unCommitedEvents = new HashSet<>();

    public static void subscribe(EventListener eventListener) {
        listeners.add(eventListener);
    }

    private static void PanicOnUncommitedEvent(String event, EntityKey<?> key) {
        log.error("Uncommited event {} of entity with key {}", event, key);
        throw new IllegalStateException(String.format("Uncommited event %s of entity with key %s", event, key));
    }

    private static void PanicOnUncommitedEvent(String event, Record key) {
        log.error("Uncommited event {} of entity with key {}", event, key);
        throw new IllegalStateException(String.format("Uncommited event %s of entity with key %s", event, key));
    }


    public static void emitEvent(@NotNull Event event) {
        /*
        WIP event system.
        switch (event) {
            case Event.NewEntityDraft draft -> unCommitedEvents.add(draft);
            case Event.NewEntity created -> {
                Event.NewEntityDraft matched = null;
                for (Event uncommitedEvent : unCommitedEvents) {
                    if (uncommitedEvent instanceof Event.NewEntityDraft draft) {
                        if (created.matches(draft)) {
                            matched = draft;
                        }
                    }
                    if (matched != null) {
                        unCommitedEvents.remove(uncommitedEvent);
                    }else PanicOnUncommitedEvent(created.getClass().getSimpleName(), created.record());
                }
            }

            case Event.DeletedEntityDraft draft -> unCommitedEvents.add(draft);
            case Event.DeletedEntity del -> {
                Event.DeletedEntityDraft matched = null;
                for (Event uncommitedEvent : unCommitedEvents) {
                    if (uncommitedEvent instanceof Event.DeletedEntityDraft draft) {
                        if (del.matches(draft)) {
                            matched = draft;
                            break;
                        }
                    }
                }
                if (matched != null) {
                    unCommitedEvents.remove(matched);
                } else PanicOnUncommitedEvent(del.getClass().getSimpleName(), del.key());
            }

            default -> {}
        }
        */
        for (EventListener eventListener : listeners) {
            eventListener.onEvent(event);
        }
    }


}
