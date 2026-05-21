package chechelpo.frplm.domain.connection.llm.utils;

import chechelpo.frplm.domain.connection.llm.LLMBackend;
import chechelpo.frplm.domain.connection.llm.utils.generationRequest.ChatChoice;
import chechelpo.frplm.domain.connection.llm.utils.generationRequest.ChatCompletionRequest;
import chechelpo.frplm.domain.connection.llm.utils.generationRequest.ChatCompletionResponse;
import chechelpo.frplm.domain.connection.llm.utils.generationRequest.ChatMessage;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.ApiHosts;
import chechelpo.frplm.jooq.generated.tables.records.ApiHostsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.List;

public final class NanoGPT extends LLMConnection {
    NanoGPT(EntityKey<LlmConnectionRecord> key, LLMRepository repository) {
        super(key, repository);
    }
    private enum NanoGPTEndpoint {
        MODELS("/api/v1/models"),
        MODELS_DETAILED("/api/v1/models?detailed=true"),
        SUBSCRIPTION_MODELS("/api/subscription/v1/models"),
        SUBSCRIPTION_MODELS_DETAILED("/api/subscription/v1/models?detailed=true"),
        PAID_MODELS("/api/paid/v1/models"),
        PAID_MODELS_DETAILED("/api/paid/v1/models?detailed=true"),
        CHAT_COMPLETION("api/v1/chat/completions")
        ;
        private final String path;

        NanoGPTEndpoint(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ModelsResponse(
            String object,
            List<NanoGPTModelInfo> data
    ) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NanoGPTModelInfo(
            @JsonProperty("id")
            String modelID,
            String object,
            Long created,

            @JsonProperty("owned_by")
            String ownedBy,

            @Nullable String name,
            @Nullable String description,

            @JsonProperty("context_length")
            @Nullable Integer contextLength,

            @JsonProperty("max_output_tokens")
            @Nullable Integer maxOutputTokens,

            @Nullable Capabilities capabilities,
            @Nullable Pricing pricing,

            @JsonProperty("icon_url")
            @Nullable String iconUrl,

            @JsonProperty("cost_estimate")
            @Nullable Object costEstimate,

            @Nullable String category
    ) implements ModelInfo {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Capabilities(
                boolean vision,
                boolean reasoning,

                @JsonProperty("tool_calling")
                boolean toolCalling,

                @JsonProperty("parallel_tool_calls")
                boolean parallelToolCalls,

                @JsonProperty("structured_output")
                boolean structuredOutput,

                @JsonProperty("pdf_upload")
                boolean pdfUpload
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Pricing(
                @Nullable Double prompt,
                @Nullable Double completion,
                @Nullable String currency,
                @Nullable String unit
        ) {}
    }

    @Override
    public @NotNull List<ModelInfo> models() {
        try {
            ModelsResponse response = this.newRestClient()
                    .get()
                    .uri(NanoGPTEndpoint.MODELS_DETAILED.path())
                    .retrieve()
                    .body(ModelsResponse.class);

            if (response == null || response.data() == null) {
                throw new IllegalStateException("NanoGPT models response was empty");
            }

            return List.copyOf(response.data());
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "NanoGPT models request failed with HTTP "
                            + e.getStatusCode()
                            + ": "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    @Override
    public @NotNull String generateSingle(@NotNull String prompt) {
        return generate(List.of(ChatMessage.user(prompt)));
    }

    public @NotNull String generate(@NotNull List<ChatMessage> messages) {
        String model = getModelID();

        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                messages,
                false
        );

        try {
            ChatCompletionResponse response = this.newRestClient().post()
                    .uri(NanoGPTEndpoint.CHAT_COMPLETION.path())
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException("LLM response has no choices");
            }

            ChatChoice firstChoice = response.choices().getFirst();

            if (firstChoice.message() == null || firstChoice.message().content() == null) {
                throw new IllegalStateException("LLM response has no message content");
            }

            return firstChoice.message().content();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "OpenAI-compatible LLM request failed with HTTP "
                            + e.getStatusCode()
                            + ": "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    @Override
    public @NotNull String generate(@NotNull ChatCompletionRequest request) {
        return "";
    }

    @Override
    protected @NotNull EntityKey<ApiHostsRecord> apiHost() {
        return EntityKey.of(ApiHosts.API_HOSTS.ID, LLMBackend.NANOGPT.stable_id);
    }

    @Override
    public URI getHostURI() {
        return LLMBackend.NANOGPT.host;
    }
}
