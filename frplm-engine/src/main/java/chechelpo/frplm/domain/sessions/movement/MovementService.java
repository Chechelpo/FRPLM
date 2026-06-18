package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.exceptions.runtime.InvalidMove;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Service
class MovementService extends EntityService<MovementsRecord, MovementStore> {

    MovementService(
            @NotNull MovementStore store,
            @NotNull EventBus eventBus)
    {
        super(store, eventBus);
    }

    public Integer getLocationBeforeTick(int characterId, int sessionId, int tick) {
        return store.getLocationBeforeTick(characterId, sessionId, tick);
    }

    @Transactional
    void registerMovementChange(CurrentLocationsRecord previous, int atTick)
            throws InvalidMove
    {
        log.trace("Registering movement change event");
        EntityKey<MovementsRecord> movementKey = EntityKey.<MovementsRecord>builder()
                .set(MOVEMENTS.SESSION_ID, previous.getSessionId())
                .set(MOVEMENTS.AT_TICK, atTick)

                .set(MOVEMENTS.CHARACTER_ID, previous.getCharacterId())
                .build();
        if (exists(movementKey)) return;

        this.createAndGet(
                EntityDataPayload.<MovementsRecord>builder()
                            .set(MOVEMENTS.SESSION_ID, previous.getSessionId())
                            .set(MOVEMENTS.AT_TICK, atTick)

                            .set(MOVEMENTS.CHARACTER_ID, previous.getCharacterId())

                            .set(MOVEMENTS.WORLD_ID, previous.getWorldId())
                            .set(MOVEMENTS.PREVIOUS_LOCATION_ID, previous.getLocationId())

                            .build()
            );
    }

    public void rollbackLatestMovementsOf(int sessionId, int tick_num){
        store.rollbackLatestMovementAt(sessionId, tick_num);
    }
}
