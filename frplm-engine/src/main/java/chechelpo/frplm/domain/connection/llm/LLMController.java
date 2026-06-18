package chechelpo.frplm.domain.connection.llm;

import chechelpo.frplm.domain.connection.api_keys.SecretService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.NotInitialized;
import chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.openai_compatible.ChatCompletionResponse;
import chechelpo.frplm.utils.integrations.Models;
import chechelpo.frplm.utils.generation.OpenAICompatible;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static chechelpo.frplm.domain.EntityTypes.LLM_CONNECTION_URL;
import static chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;

@RestController
@RequestMapping(LLM_CONNECTION_URL)
final class LLMController extends EntityController<LlmConnectionRecord, LLMService> {
    private final SecretService secretService;

    LLMController(LLMService service, SecretService secretService) {
        super(service);
        this.secretService = secretService;
    }

    record TestResponse(boolean status, String message){}
    @GetMapping("/test")
    public ResponseEntity<TestResponse> test(@RequestParam Map<String, Object> params) throws EntityNotFound {
        EntityKey<LlmConnectionRecord> key = extractKey(params);

        LlmConnectionRecord record = service.find(key)
                .orElseThrow(() -> new EntityNotFound("Could not find connection to test", Severity.USER));
        ChatCompletionRequest request = ChatCompletionRequest.getTestMessage(
                service.getValueOf(LLM_CONNECTION.MODEL, key)
                        .orElseThrow(() -> new NotInitialized("Connection model not configured", Severity.USER))
        );

        LLMBackend backend = LLMBackend.get(record.getHostId());
        log.debug("Testing connection");
        ChatCompletionResponse response =  OpenAICompatible.generateNonStreaming(
                backend.host,
                record,
                request,
                secretService
        );
        return ResponseEntity.ok(
            new TestResponse(true, response.choices().getFirst().message().content())
        );
    }

    @GetMapping("/models")
    public ResponseEntity<Models.ModelResponses> models(@RequestParam Map<String, Object> params) throws EntityNotFound {
        EntityKey<LlmConnectionRecord> key = extractKey(params);
        LlmConnectionRecord llm = service.find(key)
                    .orElseThrow(() -> new EntityNotFound("Could not find connection to fetch models from", Severity.USER));

        return ResponseEntity.ok(Models.fetch(llm, secretService));
    }
}
