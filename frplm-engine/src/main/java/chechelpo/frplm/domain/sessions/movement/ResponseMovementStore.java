package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.jooq.generated.tables.records.ResponseLocationChangesRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.RESPONSE_LOCATION_CHANGES;

@Store
final class ResponseMovementStore extends EntityStore<ResponseLocationChangesRecord> {
    ResponseMovementStore(@NotNull DSLContext ctx) {
        super(ctx, RESPONSE_LOCATION_CHANGES, EntityTypes.Types.RESPONSE_MOVEMENTS);
    }

    public @NotNull List<ResponseLocationChangesRecord> getResponseMovements(int sessionId, int tick_num, short response_num){
        return ctx.selectFrom(main_table)
                    .where(
                            RESPONSE_LOCATION_CHANGES.SESSION_ID.eq(sessionId)
                                    .and(RESPONSE_LOCATION_CHANGES.TICK_NUM.eq(tick_num))
                                    .and(RESPONSE_LOCATION_CHANGES.RESPONSE_NUM.eq(response_num))
                    )
                    .fetch();
    }
}
