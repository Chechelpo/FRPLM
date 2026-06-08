package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Store
final class CurrentLocationStore extends EntityStore<CurrentLocationsRecord> {
    CurrentLocationStore(@NotNull DSLContext ctx) {
        super(ctx, CURRENT_LOCATIONS, EntityTypes.Types.CURRENT_LOCATIONS);
    }

    public void rollbackSessionTo(int sessionID, int tick) {
        ctx.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);

            var movementsToUndo = tx.selectFrom(MOVEMENTS)
                    .where(MOVEMENTS.SESSION_ID.eq(sessionID))
                    .and(MOVEMENTS.AT_TICK.gt(tick))
                    .orderBy(MOVEMENTS.AT_TICK.desc())
                    .fetch();

            for (MovementsRecord movement : movementsToUndo) {
                tx.update(CURRENT_LOCATIONS)
                        .set(CURRENT_LOCATIONS.TICK_NUM, tick)
                        .set(CURRENT_LOCATIONS.WORLD_ID, movement.getWorldId())
                        .set(CURRENT_LOCATIONS.LOCATION_ID, movement.getLocationId())
                        .where(CURRENT_LOCATIONS.SESSION_ID.eq(sessionID))
                        .and(CURRENT_LOCATIONS.CHARACTER_ID.eq(movement.getCharacterId()))
                        .execute();
            }

            tx.deleteFrom(MOVEMENTS)
                    .where(MOVEMENTS.SESSION_ID.eq(sessionID))
                    .and(MOVEMENTS.AT_TICK.gt(tick))
                    .execute();

            tx.deleteFrom(LLM_GEN)
                    .where(LLM_GEN.SESSION_ID.eq(sessionID))
                    .and(LLM_GEN.TICK_NUM.gt(tick))
                    .execute();

            tx.deleteFrom(MESSAGES)
                    .where(MESSAGES.SESSION_ID.eq(sessionID))
                    .and(MESSAGES.TICK_NUM.gt(tick))
                    .execute();
        });
    }

    public @NotNull List<CurrentLocationsRecord> getAtLocation(
            int sessionID,
            int locationID
    ) {
        return ctx.selectFrom(main_table)
                .where(
                        CURRENT_LOCATIONS.SESSION_ID.eq(sessionID)
                                .and(CURRENT_LOCATIONS.LOCATION_ID.eq(locationID))
                )
                .fetch();
    }

}
