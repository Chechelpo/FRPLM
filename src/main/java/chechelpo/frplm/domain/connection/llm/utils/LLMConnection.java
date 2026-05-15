package chechelpo.frplm.domain.connection.llm.utils;

import chechelpo.frplm.domain.connection.llm.utils.generationRequest.ChatCompletionRequest;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import chechelpo.frplm.jooq.generated.tables.records.ApiKeysRecord;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;

/**
 * Live handle for an LLM connection
 */
public sealed abstract class LLMConnection permits NanoGPT {
    private final EntityKey<LlmConnectionRecord> id;
    private final LLMRepository repository;

    LLMConnection(LLMRepository repository, EntityKey<LlmConnectionRecord> key) {
        this.id = key;
        this.repository = repository;
    }
    public boolean test(){
        generateSingle("Are you there (Yes/No)?");
        return true;
    }

    public abstract @NotNull String generateSingle(@NotNull String prompt);
    public abstract @NotNull String generate(@NotNull ChatCompletionRequest request);
    public abstract @NotNull List<ModelInfo> models();

    /** @return a new Rest client with its host already added */
    @CheckReturnValue
    protected @NotNull WebClient newWebClient() {
        return WebClient
                .builder()
                .baseUrl(this.getHostURI().toString())
                .build();
    }
    /** @return a new rest client with the host already added */
    @CheckReturnValue
    protected @NotNull RestClient newRestClient() {
        return RestClient.builder()
                .baseUrl(this.getHostURI().toString())
                .defaultHeaders(headers -> headers.setBearerAuth(getKey()))
                .build();
    }

    protected @NotNull EntityKey<ApiKeysRecord> apiKey(){
        return EntityKey.of(
                API_KEYS.KEY_ID,
                repository.getFromLLM(LLM_CONNECTION.API_KEY, id)
        );
    }
    protected @NotNull EntityKey<ApiHostsRecord> apiHost(){
        return EntityKey.of(
                API_HOSTS.ID,
                repository.getFromSecrets(API_KEYS.HOST_ID, apiKey())
        );
    }

    public final String getModelID(){
        return repository.getFromLLM(LLM_CONNECTION.MODEL, id);
    }
    public URI getHostURI(){
        return URI.create(repository.getFromHosts(API_HOSTS.HOST_URL, apiHost()));
    }
    final @NotNull String getKey(){
        return repository.getDecryptedKey(apiKey());
    }
}
