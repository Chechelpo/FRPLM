package chechelpo.frplm.events;

import chechelpo.frplm.config.controllers.EntityTypes;
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