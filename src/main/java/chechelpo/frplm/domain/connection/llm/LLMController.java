package chechelpo.frplm.domain.connection.llm;

import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.domain.EntityTypes.LLM_CONNECTION_URL;

@RestController
@RequestMapping(LLM_CONNECTION_URL)
final class LLMController extends ABSEntityController<LlmConnectionRecord, LLMService> {
    LLMController(LLMService service) {
        super(service);
    }
}
