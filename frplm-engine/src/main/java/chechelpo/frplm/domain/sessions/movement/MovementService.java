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
public class MovementService extends EntityService<MovementsRecord, MovementStore> {

    MovementService(
            @NotNull MovementStore store,
            @NotNull EventBus eventBus)
    {
        super(store, eventBus);
    }


    public void goBack(int sessionID, int tick){
        throw new UnsupportedOperationException("Not implemented yet");
    }



    void registerMovementChange(EntityKey<CurrentLocationsRecord> target, EntityDataPayload<CurrentLocationsRecord> data)
            throws InvalidMove
    {
        log.trace("Registering movement change event");
        try{
            int movedCharacterID = target.getValue(CURRENT_LOCATIONS.CHARACTER_ID);
            int toLocationID = data.requireValue(CURRENT_LOCATIONS.LOCATION_ID);
            int atTick = data.requireValue(CURRENT_LOCATIONS.TICK_NUM);
            log.debug("Registering movement change event of character id {} at tick {} to location ID {}",
                    movedCharacterID,
                    atTick,
                    toLocationID
            );

            this.unsafeCreate(EntityDataPayload.<MovementsRecord>builder()
                            .set(MOVEMENTS.SESSION_ID, target.getValue(CURRENT_LOCATIONS.SESSION_ID))
                            .set(MOVEMENTS.CHARACTER_ID, movedCharacterID)
                            .set(MOVEMENTS.WORLD_ID, data.requireValue(CURRENT_LOCATIONS.WORLD_ID))
                            .set(MOVEMENTS.LOCATION_ID, toLocationID)
                            .set(MOVEMENTS.AT_TICK, atTick)
                            .build()
            );

        } catch (Exception e){
            log.error("Error while registering movement change event, rolling back to last known location", e);
            throw new InvalidMove("Invalid movement change");
        }
    }
}
