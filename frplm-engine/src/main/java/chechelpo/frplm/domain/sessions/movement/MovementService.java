package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.exceptions.runtime.InvalidMove;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

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

    void registerMovementChange(CurrentLocationsRecord previous, int atTick)
            throws InvalidMove
    {
        log.trace("Registering movement change event");
        try{
            this.unsafeCreate(EntityDataPayload.<MovementsRecord>builder()
                            .set(MOVEMENTS.SESSION_ID, previous.getSessionId())
                            .set(MOVEMENTS.CHARACTER_ID, previous.getCharacterId())
                            .set(MOVEMENTS.WORLD_ID, previous.getWorldId())
                            .set(MOVEMENTS.LOCATION_ID, previous.getLocationId())
                            .set(MOVEMENTS.AT_TICK, atTick)
                            .build()
            );
        } catch (Exception e){
            log.error("Error while registering movement change event, rolling back to last known location", e);
            throw new InvalidMove("Invalid movement change: \n" + e.getMessage());
        }
    }
}
