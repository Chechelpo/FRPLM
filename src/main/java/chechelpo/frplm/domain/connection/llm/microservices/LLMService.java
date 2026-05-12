package chechelpo.frplm.domain.connection.llm.microservices;

import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.springframework.stereotype.Service;

@Service
public final class LLMService extends ABSEntityService<LlmConnectionRecord, LLMStore> {
    LLMService(LLMStore store) {
        super(store);
    }
}
