package chechelpo.frplm.domain.connection.llm.microservices;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.springframework.stereotype.Service;

@Service
public class LLMService extends EntityService<LlmConnectionRecord, LLMStore> {
    LLMService(LLMStore store, EventBus eventBus) {
        super(store, eventBus);
    }
}
