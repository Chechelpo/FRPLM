package chechelpo.frplm.events;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import org.jooq.TableRecord;

public sealed interface Event permits
        Event.DeletedEntityDraft, Event.DeletedEntity,
        Event.NewEntityDraft, Event.NewEntity,
        Event.UpdatedEntity
{
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Entity Events.
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Signals that a new entity will be created.
     * @param type Entity type
     * @param eventIdentifier the id that will come with the next draft
     * @param initialKey initial key data
     * @param initialData initial payload
     * @apiNote This event must only be emitted <b>AFTER THE DATA HAS BEEN CHECKED</b>. Other systems will expect this draft
     * to succeed 99.9% of times, so you better check the data.
     */
    record NewEntityDraft(
            EntityTypes.Types type,
            int eventIdentifier,
            EntityKey<?> initialKey,
            EntityDataPayload<?> initialData
    ) implements Event {}
    record NewEntity(EntityTypes.Types type /*, int eventIdentifier*/,TableRecord<?> record) implements Event {
        /*
        @Contract(pure = true)
        public boolean matches(@NotNull NewEntityDraft draft) {
            return eventIdentifier == draft.eventIdentifier && this.type.equals(draft.type);
        }
        */

    }


    record DeletedEntityDraft(EntityTypes.Types type, /*int eventIdentifier,*/ EntityKey<?> key) implements Event{}
    record DeletedEntity(EntityTypes.Types type, /*int eventIdentifier,*/ EntityKey<?> key) implements Event{
        /*
        @Contract(pure = true)
        public boolean matches(DeletedEntityDraft draft) {
            return eventIdentifier == draft.eventIdentifier && this.type.equals(draft.type);
        }*/
    }


    record UpdatedEntity(EntityTypes.Types type, EntityDataPayload<?> newData) implements Event{}
}