package io.github.chechelpo.frplm.utils.integrations;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.connection.api_hosts.HostService;
import io.github.chechelpo.frplm.domain.connection.api_keys.SecretService;
import io.github.chechelpo.frplm.domain.connection.llm.LLMBackend;
import io.github.chechelpo.frplm.jooq.generated.tables.ApiHosts;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionResponse;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class T2TClient {
    private static EnumMap<LLMBackend, LLMKnownHost> hosts  = new EnumMap<>(Map.of(
            LLMBackend.NANO_GPT, new NanoGPT(),
            LLMBackend.OPEN_ROUTER, new OpenRouter()
    ));

    private final SecretService secrets;
    private final HostService apiHosts;

    public T2TClient(SecretService secrets, HostService hosts){
        this.secrets = secrets;
        this.apiHosts = hosts;
    }

    private @NonNull Optional<LLMKnownHost> getHostFor(@NonNull LlmConnectionRecord connection){
        return Optional.ofNullable(
                hosts.get(LLMBackend.get(connection.getHostId()))
        );
    }
    @Contract("_ -> new")
    private @NonNull OpenAICompatible getOpenAIClient(@NonNull LlmConnectionRecord con){
        return new OpenAICompatible(
                apiHosts.find(EntityKey.of(ApiHosts.API_HOSTS.ID, con.getHostId().intValue())).orElseThrow().getHostUrl()
        );
    }

    public Optional<ChatCompletionResponse> generate(ChatCompletionRequest request, LlmConnectionRecord withConnection){
        Optional<LLMKnownHost> knownHost = getHostFor(withConnection);
        String apiKey = secrets.getKeyForConnectionHost(withConnection).orElse(null);

        if (knownHost.isPresent()) return knownHost.get().generate(request, apiKey);
        return getOpenAIClient(withConnection).generateNonStreaming(request, secrets.getKeyForConnectionHost(withConnection).orElse(null));
    }
    public ModelResponses modelsOf(LlmConnectionRecord connection){
        Optional<LLMKnownHost> knownHost =  getHostFor(connection);
        String apiKey = secrets.getKeyForConnectionHost(connection).orElse(null);
        if (knownHost.isPresent()) return knownHost.get().models(apiKey);
        return getOpenAIClient(connection).getModels(apiKey);
    }

    public Integer tokenize(String text, LlmConnectionRecord withConnection){
        return null;
    }
}
