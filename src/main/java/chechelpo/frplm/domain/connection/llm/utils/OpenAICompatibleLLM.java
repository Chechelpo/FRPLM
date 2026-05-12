package chechelpo.frplm.domain.connection.llm.utils;

import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.utils.endpoints.OpenAIEndpoints;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

public abstract sealed class OpenAICompatibleLLM extends LLMConnection permits NanoGPT {
    private static final Duration GENERATION_TIMEOUT = Duration.ofSeconds(270);

    OpenAICompatibleLLM(
            LLMRepository repository,
            EntityKey<LlmConnectionRecord> key
    ) {
        super(repository, key);
    }

    /** Standard OpenAI-compatible chat-completion message. */
    public record ChatMessage(
            @NotNull Role role,
            @NotNull String content
    ) {
        @Contract("_ -> new")
        public static @NotNull ChatMessage user(@NotNull String content) {
            return new ChatMessage(Role.USER, content);
        }

        @Contract("_ -> new")
        public static @NotNull ChatMessage assistant(@NotNull String content) {
            return new ChatMessage(Role.ASSISTANT, content);
        }

        @Contract("_ -> new")
        public static @NotNull ChatMessage system(@NotNull String content) {
            return new ChatMessage(Role.SYSTEM, content);
        }

        public enum Role {
            USER("user"),
            ASSISTANT("assistant"),
            SYSTEM("system");

            private final String wireValue;

            Role(String wireValue) {
                this.wireValue = wireValue;
            }

            @JsonValue
            public String wireValue() {
                return wireValue;
            }
        }
    }

    protected record ChatCompletionRequest(
            @NotNull String model,
            @NotNull List<ChatMessage> messages,
            boolean stream,

            @JsonProperty("max_tokens")
            Integer maxTokens
    ) {}

    @Override
    public final @NotNull String generate(@NotNull String prompt) {
        return generate(List.of(ChatMessage.user(prompt)));
    }

    public final @NotNull String generate(@NotNull List<ChatMessage> messages) {
        return complete(messages, null);
    }

    protected final @NotNull String complete(
            @NotNull List<ChatMessage> messages,
            Integer maxTokens
    ) {
        String model = requireNonBlank(getModelID(), "LLM modelID is not configured");
        String apiKey = requireNonBlank(getKey(), "LLM API key is not configured");

        ChatCompletionRequest requestBody = new ChatCompletionRequest(
                model,
                messages,
                false,
                maxTokens
        );

        try {
            ChatCompletionResponse response = newWebClient()
                    .post()
                    .uri(OpenAIEndpoints.CHAT_COMPLETIONS.pathTemplate)
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .block(GENERATION_TIMEOUT);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException("OpenAI-compatible LLM response has no choices");
            }

            ChatChoice firstChoice = response.choices().getFirst();

            if (firstChoice.message() == null || firstChoice.message().content() == null) {
                throw new IllegalStateException("OpenAI-compatible LLM response has no message content");
            }

            return firstChoice.message().content();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "OpenAI-compatible LLM request failed with HTTP "
                            + e.getStatusCode()
                            + ": "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    protected final boolean completionWorks() {
        complete(
                List.of(ChatMessage.user("Are you there (Yes/No)?")),
                4
        );
        return true;
    }

    private static @NotNull String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }

        return value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(
            List<ChatChoice> choices
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatChoice(
            ChatMessage message,

            @JsonProperty("finish_reason")
            String finishReason
    ) {}
}