package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.MOVEMENTS;

@Store
final class MovementStore extends EntityStore<MovementsRecord> {
    MovementStore(@NotNull DSLContext ctx) {
        super(ctx, MOVEMENTS, EntityTypes.Types.MOVEMENTS);
    }

    public Integer getLocationBeforeTick(int characterId, int sessionId, int tick) {
        return ctx.select(MOVEMENTS.LOCATION_ID)
                .from(MOVEMENTS)
                .where(MOVEMENTS.CHARACTER_ID.eq(characterId))
                .and(MOVEMENTS.SESSION_ID.eq(sessionId))
                .and(MOVEMENTS.AT_TICK.lt(tick))
                .orderBy(MOVEMENTS.AT_TICK.desc())
                .limit(1)
                .fetchOne(MOVEMENTS.LOCATION_ID);
    }
}
