package chechelpo.frplm.domain.sessions.core.microservices;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@Store
final class SessionStore extends EntityStore<SessionsRecord> {
    SessionStore(@NotNull DSLContext ctx) {
        super(ctx, SESSIONS, EntityTypes.Types.SESSIONS);
    }
}
