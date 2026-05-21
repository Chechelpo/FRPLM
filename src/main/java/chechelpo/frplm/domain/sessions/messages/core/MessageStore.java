package chechelpo.frplm.domain.sessions.messages.core;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.MESSAGES;

@Store
final class MessageStore extends EntityStore<MessagesRecord> {
    MessageStore(@NotNull DSLContext ctx) {
        super(ctx, MESSAGES, EntityTypes.Types.MESSAGES);
    }
}
