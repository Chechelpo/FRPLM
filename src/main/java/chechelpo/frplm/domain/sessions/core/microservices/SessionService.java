package chechelpo.frplm.domain.sessions.core.microservices;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.springframework.stereotype.Service;

@Service
public class SessionService extends EntityService<SessionsRecord, SessionStore> {
    SessionService(SessionStore store, EventBus eventBus) {
        super(store, eventBus);
    }
}
