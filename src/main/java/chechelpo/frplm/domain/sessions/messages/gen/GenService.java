package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.LlmGenRecord;
import org.springframework.stereotype.Service;

@Service
public class GenService extends EntityService<LlmGenRecord, GenStore> {
    GenService(GenStore store, EventBus eventBus) {
        super(store, eventBus);
    }
}
