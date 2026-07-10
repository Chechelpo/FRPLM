package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.List;

import static io.github.chechelpo.frplm.domain.sessions.messages.MessageService.FIRST_MESSAGE_TICK_NUM;
import static chechelpo.frplm.jooq.generated.Tables.CURRENT_LOCATIONS;
import static chechelpo.frplm.jooq.generated.Tables.MOVEMENTS;

@Store
final class MovementStore extends EntityStore<MovementsRecord> {
    MovementStore(@NotNull DSLContext ctx) {
        super(ctx, MOVEMENTS, EntityConfigs.Types.MOVEMENTS);
    }

    public Integer getLocationBeforeTick(int characterId, int sessionId, int tick) {
        return ctx.select(MOVEMENTS.PREVIOUS_LOCATION_ID)
                .from(MOVEMENTS)
                .where(MOVEMENTS.CHARACTER_ID.eq(characterId))
                .and(MOVEMENTS.SESSION_ID.eq(sessionId))
                .and(MOVEMENTS.AT_TICK.lt(tick))
                .orderBy(MOVEMENTS.AT_TICK.desc())
                .limit(1)
                .fetchOne(MOVEMENTS.PREVIOUS_LOCATION_ID);
    }
    /**
     * Rollback movements made on a particular tick, sacrificing them to transform them to a current location
     * Assumes movements contains a backlog of movement from location at tick.
     */
    public void rollbackLatestMovementAt(int sessionId, int tick){
        ctx.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);
            List<MovementsRecord> movementsRecords = tx.deleteFrom(MOVEMENTS)
                    .where(MOVEMENTS.SESSION_ID.eq(sessionId)
                            .and(MOVEMENTS.AT_TICK.eq(tick))
                    )
                    .returning()
                    .fetch();

            for (MovementsRecord movement : movementsRecords) {
                Integer lastMovementTick = tx.selectFrom(MOVEMENTS)
                                .where(MOVEMENTS.SESSION_ID.eq(sessionId)
                                        .and(MOVEMENTS.CHARACTER_ID.eq(movement.getCharacterId()))
                                )
                                .orderBy(MOVEMENTS.AT_TICK.desc())
                                .limit(1)
                                .fetchOne(MOVEMENTS.AT_TICK);
                //Could happen if this was the first movement
                if (lastMovementTick == null)
                    lastMovementTick = FIRST_MESSAGE_TICK_NUM;

                tx.update(CURRENT_LOCATIONS)
                        .set(CURRENT_LOCATIONS.LOCATION_ID, movement.getPreviousLocationId())
                        .set(CURRENT_LOCATIONS.TICK_NUM, lastMovementTick)
                        .where(CURRENT_LOCATIONS.SESSION_ID.eq(sessionId)
                                .and(CURRENT_LOCATIONS.TICK_NUM.eq(tick))
                                .and(CURRENT_LOCATIONS.CHARACTER_ID.eq(movement.getCharacterId()))
                        )
                        .execute();
            }
        });
    }
}
