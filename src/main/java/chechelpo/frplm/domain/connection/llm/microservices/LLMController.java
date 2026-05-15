package chechelpo.frplm.domain.connection.llm.microservices;

import chechelpo.frplm.domain.connection.llm.utils.LLMConnection;
import chechelpo.frplm.domain.connection.llm.utils.LLMFactory;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static chechelpo.frplm.domain.EntityTypes.LLM_CONNECTION_URL;

@RestController
@RequestMapping(LLM_CONNECTION_URL)
final class LLMController extends ABSEntityController<LlmConnectionRecord, LLMService> {
    private final LLMFactory llmFactory;
    LLMController(LLMService service, LLMFactory factory) {
        super(service);
        this.llmFactory = factory;
    }

    @GetMapping("/test")
    public ResponseEntity<Boolean> test(@RequestParam Map<String, Object> params) {
        EntityKey<LlmConnectionRecord> key = extractKey(params);
        service.coerceFullKey(key);

        LLMConnection connection = llmFactory.getOf(key);
        return ResponseEntity.ok(connection.test());
    }
}
