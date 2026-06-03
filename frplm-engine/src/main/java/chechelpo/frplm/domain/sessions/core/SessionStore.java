package chechelpo.frplm.domain.sessions.core;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.TableField;

import static chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@Store
final class SessionStore extends EntityStore<SessionsRecord> {
    SessionStore(@NotNull DSLContext ctx) {
        super(ctx, SESSIONS, EntityTypes.Types.SESSIONS);
    }

    @SuppressWarnings("DataFlowIssue")
    boolean decrementTickNum(int sessionID){
        return ctx.update(SESSIONS)
                .set(SESSIONS.CURRENT_TICK,
                        ctx.select(SESSIONS.CURRENT_TICK)
                                .from(SESSIONS)
                                .where(SESSIONS.ID.eq(sessionID))
                                .fetchOne(SESSIONS.CURRENT_TICK)
                        - 1
                )
                .where(SESSIONS.ID.eq(sessionID))
                .execute() == 1;
    }
}
