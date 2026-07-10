package chechelpo.frplm.utils.tokenizers;

import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.connection.llm.LLMService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.NotInitialized;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.WebAsyncTask;

import static chechelpo.frplm.domain.EntityTypes.API_BASE;
import static chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;

@RestController
@RequestMapping(API_BASE + "/tokenizer")
final class TokenizerController {
    private final LLMService lLMService;
    private final TokenizerService tokenizerService;

    TokenizerController(LLMService lLMService, TokenizerService tokenizerService){
        this.lLMService = lLMService;
        this.tokenizerService = tokenizerService;
    }

    /** Tokenizes with timeout. Increment timeout if necessary, but NEVER delete the timeout */
    @PostMapping("/tokenize")
    public WebAsyncTask<ResponseEntity<Integer>> tokenize(
            @RequestParam int connectionId,
            @RequestBody String text
    ) {
        long timeoutMillis = 5_000L;

        WebAsyncTask<ResponseEntity<Integer>> task =
                new WebAsyncTask<>(timeoutMillis, () -> {

                    LlmConnectionRecord connection = lLMService
                            .find(EntityKey.of(LLM_CONNECTION.ID, connectionId))
                            .orElseThrow(() -> new EntityNotFound(
                                    "No tokenizer connection with id " + connectionId,
                                    Severity.USER
                            ));

                    String modelId = connection.getModel();

                    if (modelId == null) {
                        throw new NotInitialized(
                                "Tokenizer connection has no model id selected",
                                Severity.USER
                        );
                    }

                    int tokenCount = tokenizerService.tokenCount(modelId, text);

                    return ResponseEntity.ok(tokenCount);
                });

        task.onTimeout(() ->
                ResponseEntity
                        .status(HttpStatus.GATEWAY_TIMEOUT)
                        .build()
        );

        return task;
    }
}
