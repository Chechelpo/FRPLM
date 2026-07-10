package io.github.chechelpo.frplm.domain.sessions.core;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@Store
final class SessionStore extends EntityStore<SessionsRecord> {
    SessionStore(@NotNull DSLContext ctx) {
        super(ctx, SESSIONS, EntityConfigs.Types.SESSIONS);
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
