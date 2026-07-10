package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Store
final class CurrentLocationStore extends EntityStore<CurrentLocationsRecord> {
    CurrentLocationStore(@NotNull DSLContext ctx) {
        super(ctx, CURRENT_LOCATIONS, EntityConfigs.Types.CURRENT_LOCATIONS);
    }

    /**
     * Rollbacks the locations where entities where before this tick number.
     * <p>
     *     Essentially, the movement record with the highest tick num is sacrificed and transformed to be the new current location
     *     of that particular character. After this, that particular movement record is erased.
     * </p>
     * <h3>Algorithm:</h3>
     * <pre>
     *     1. Fetches the latest (highest tick_num) movement records of each entity with a tick > tick_num.
     *     2. Converts those movement history records into the new current locations.
     *     3. Erases those particular movement history.
     * </pre>
     * @param sessionID id
     * @param tick to go back to
     */
    public void rollbackSessionTo(int sessionID, int tick) {
        ctx.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);

            boolean targetTickExists = tx.fetchExists(
                    tx.selectOne()
                            .from(MESSAGES)
                            .where(MESSAGES.SESSION_ID.eq(sessionID))
                            .and(MESSAGES.TICK_NUM.eq(tick))
            );

            if (!targetTickExists) {
                throw new IllegalStateException(
                        "Cannot rollback session " + sessionID +
                                " to tick " + tick +
                                ": target tick does not exist in MESSAGES."
                );
            }

            while (true) {
                Integer latestMovementTick = tx.select(DSL.max(MOVEMENTS.AT_TICK))
                        .from(MOVEMENTS)
                        .where(MOVEMENTS.SESSION_ID.eq(sessionID))
                        .and(MOVEMENTS.AT_TICK.gt(tick))
                        .fetchOne(0, Integer.class);

                if (latestMovementTick == null) {
                    break;
                }

                Result<MovementsRecord> movementsToPop = tx.selectFrom(MOVEMENTS)
                        .where(MOVEMENTS.SESSION_ID.eq(sessionID))
                        .and(MOVEMENTS.AT_TICK.eq(latestMovementTick))
                        .fetch();

                for (MovementsRecord movement : movementsToPop) {
                    tx.insertInto(CURRENT_LOCATIONS)
                            .set(CURRENT_LOCATIONS.SESSION_ID, sessionID)
                            .set(CURRENT_LOCATIONS.TICK_NUM, tick)
                            .set(CURRENT_LOCATIONS.CHARACTER_ID, movement.getCharacterId())
                            .set(CURRENT_LOCATIONS.WORLD_ID, movement.getWorldId())
                            .set(CURRENT_LOCATIONS.LOCATION_ID, movement.getPreviousLocationId())
                            .onDuplicateKeyUpdate()
                            .set(CURRENT_LOCATIONS.TICK_NUM, tick)
                            .set(CURRENT_LOCATIONS.WORLD_ID, movement.getWorldId())
                            .set(CURRENT_LOCATIONS.LOCATION_ID, movement.getPreviousLocationId())
                            .execute();
                }

                tx.deleteFrom(MOVEMENTS)
                        .where(MOVEMENTS.SESSION_ID.eq(sessionID))
                        .and(MOVEMENTS.AT_TICK.eq(latestMovementTick))
                        .execute();
            }
        });
    }

    public void rollbackLocationsToBefore(int sessionID, int deletedTick) {
        ctx.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);

            Integer previousTick = tx.select(DSL.max(MESSAGES.TICK_NUM))
                    .from(MESSAGES)
                    .where(MESSAGES.SESSION_ID.eq(sessionID))
                    .and(MESSAGES.TICK_NUM.lt(deletedTick))
                    .fetchOne(0, Integer.class);

            Result<MovementsRecord> movements = tx.selectFrom(MOVEMENTS)
                    .where(MOVEMENTS.SESSION_ID.eq(sessionID))
                    .and(MOVEMENTS.AT_TICK.eq(deletedTick))
                    .fetch();

            for (MovementsRecord movement : movements) {
                if (previousTick == null) {
                    tx.deleteFrom(CURRENT_LOCATIONS)
                            .where(CURRENT_LOCATIONS.SESSION_ID.eq(sessionID))
                            .and(CURRENT_LOCATIONS.CHARACTER_ID.eq(movement.getCharacterId()))
                            .execute();
                } else {
                    tx.insertInto(CURRENT_LOCATIONS)
                            .set(CURRENT_LOCATIONS.SESSION_ID, sessionID)
                            .set(CURRENT_LOCATIONS.TICK_NUM, previousTick)
                            .set(CURRENT_LOCATIONS.CHARACTER_ID, movement.getCharacterId())
                            .set(CURRENT_LOCATIONS.WORLD_ID, movement.getWorldId())
                            .set(CURRENT_LOCATIONS.LOCATION_ID, movement.getPreviousLocationId())
                            .onDuplicateKeyUpdate()
                            .set(CURRENT_LOCATIONS.TICK_NUM, previousTick)
                            .set(CURRENT_LOCATIONS.WORLD_ID, movement.getWorldId())
                            .set(CURRENT_LOCATIONS.LOCATION_ID, movement.getPreviousLocationId())
                            .execute();
                }

                movement.delete();
            }
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
