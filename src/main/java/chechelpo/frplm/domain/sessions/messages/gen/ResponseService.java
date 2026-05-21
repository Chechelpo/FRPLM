package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import org.springframework.stereotype.Service;

@Service
class ResponseService extends EntityService<ResponsesRecord, ResponseStore> {
    ResponseService(ResponseStore store, EventBus eventBus) {
        super(store, eventBus);
    }
}
