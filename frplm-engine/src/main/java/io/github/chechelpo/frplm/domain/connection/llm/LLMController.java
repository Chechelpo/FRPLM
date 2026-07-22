package io.github.chechelpo.frplm.domain.connection.llm;

import io.github.chechelpo.frplm.domain.connection.api_hosts.HostService;
import io.github.chechelpo.frplm.domain.connection.api_keys.SecretService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.NotInitialized;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import io.github.chechelpo.frplm.utils.integrations.ModelResponses;
import io.github.chechelpo.frplm.utils.integrations.T2TClient;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static io.github.chechelpo.frplm.jooq.generated.Tables.API_HOSTS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;
import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.LLM_CONNECTION_URL;

@RestController
@Component
@RequestMapping(LLM_CONNECTION_URL)
final class LLMController extends EntityController<LlmConnectionRecord, LLMService> {
    private final T2TClient textToTextClient;
    private final HostService hosts;
    LLMController(LLMService service, SecretService secretService, HostService hostService) {
        super(service);
        textToTextClient = new T2TClient(secretService, hostService);
        this.hosts = hostService;
    }

    record TestResponse(boolean status, String message){}
    @GetMapping("/test")
    public ResponseEntity<TestResponse> test(@RequestParam Map<String, Object> params) throws EntityNotFound {
        EntityKey<LlmConnectionRecord> key = extractKey(params);

        LlmConnectionRecord connection = service.find(key)
                .orElseThrow("Could not find connection to test", Severity.USER);
        ChatCompletionRequest request = ChatCompletionRequest.getTestMessage(
                service.getValueOf(LLM_CONNECTION.MODEL, key)
                        .orElseThrow(() -> new NotInitialized("Connection model not configured", Severity.USER))
        );

        log.debug("Testing connection");
        ChatCompletionResponse response =  textToTextClient.generate(request, connection).orElseThrow();
        return ResponseEntity.ok(
            new TestResponse(true, response.choices().getFirst().message().content())
        );
    }

    @GetMapping("/models")
    public ResponseEntity<ModelResponses> models(@RequestParam Map<String, Object> params) throws EntityNotFound {
        EntityKey<LlmConnectionRecord> key = extractKey(params);
        LlmConnectionRecord llm = service.find(key)
                    .orElseThrow("Could not find connection to fetch models from", Severity.USER);

        return ResponseEntity.ok(textToTextClient.modelsOf(llm));
    }

    record Host(int hostId, String url){}

    @GetMapping("/host/{hostId}")
    public ResponseEntity<Host> getCustomHost(@PathVariable int hostId){
        ApiHostsRecord hostsRecord = hosts.find(EntityKey.of(API_HOSTS.ID, hostId))
                .orElseThrow("No host with id " + hostId, Severity.EXPECTED);
        return ResponseEntity.ok(
                new Host(hostsRecord.getId(), hostsRecord.getHostUrl())
        );
    }
    @PutMapping("/{conId}/assignHost")
    public ResponseEntity<Host> createAndAssignHost(@PathVariable int conId, @RequestParam("url") String url){
        ApiHostsRecord hostsRecord = service.assignHost(conId, url);
        return ResponseEntity.ok(new Host(hostsRecord.getId(), hostsRecord.getHostUrl()));
    }
}
