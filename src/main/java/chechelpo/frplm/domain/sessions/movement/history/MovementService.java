package chechelpo.frplm.domain.sessions.movement.history;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import org.springframework.stereotype.Service;

@Service
public class MovementService extends EntityService<MovementsRecord, MovementStore> {
    MovementService(MovementStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    public void reset(int sessionID, int currentTick){
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
