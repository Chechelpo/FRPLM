package chechelpo.frplm.pipelines.extras;

import chechelpo.frplm.domain.connection.api_keys.SecretService;
import chechelpo.frplm.domain.connection.llm.LLMBackend;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatusCode;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static chechelpo.frplm.config.Constants.DEFAULT_LLM_TIMEOUT;

public final class Models {
    private Models() {}
    private final static String MODEL_ENDPOINT = "/api/v1/models";
    public record ModelResponses(String object, ModelResponse[] data){};
    public record ModelResponse(String id, String object, String name, Integer context_length){}

    public static ModelResponses fetch(@NotNull LlmConnectionRecord record, SecretService secretService) {
        LLMBackend backend = LLMBackend.get(record.getHostId());
        return switch (backend){
            case NANOGPT -> {
                Optional<String> apiKey = secretService.getKeyForConnectionHost(record);

                yield getNanoModels(apiKey);
            }
            case null, default -> throw new IllegalArgumentException("Couldn't fetch models");
        };
    }

    private static ModelResponses getNanoModels(Optional<String> apiKey) {
        return LLMBackend.NANOGPT.getDefaultClient().orElseThrow(() -> new IllegalStateException("NANOGPT has no default client configured"))
                .get()
                .uri(MODEL_ENDPOINT)
                .headers(headers -> apiKey
                        .map(String::trim)
                        .filter(key -> !key.isEmpty())
                        .ifPresent(headers::setBearerAuth)
                )
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        clientResponse -> clientResponse
                                .bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(errorBody -> Mono.error(
                                        new IllegalStateException(
                                                "LLM provider returned HTTP "
                                                        + clientResponse.statusCode()
                                                        + ": "
                                                        + errorBody
                                        )
                                ))
                )
                .bodyToMono(ModelResponses.class)
                .block(DEFAULT_LLM_TIMEOUT);
    }
}
