package chechelpo.frplm.utils.generation;

import chechelpo.frplm.domain.connection.api_keys.SecretService;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.openai_compatible.ChatCompletionResponse;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import static chechelpo.frplm.config.Constants.DEFAULT_LLM_TIMEOUT;
import static chechelpo.frplm.config.logging.Constants.PRINT_RESPONSE;

public final class OpenAICompatible {
    private OpenAICompatible() {}
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CHAT_COMPLETION_ENDPOINT = "/api/v1/chat/completions";

    public static @NotNull ChatCompletionResponse generateNonStreaming(
            URI host,
            LlmConnectionRecord record,
            @NotNull ChatCompletionRequest request,
            @NotNull SecretService secrets
    ) {
        if (request.configurationParameters().streaming()) throw new IllegalArgumentException("Streaming requests are not supported");

        return generateNonStreaming(host, record, OBJECT_MAPPER.writeValueAsString(request), secrets);
    }

    public static @NotNull ChatCompletionResponse generateNonStreaming(
            URI host,
            LlmConnectionRecord record,
            String request,
            @NotNull SecretService secrets
    ) {
        Objects.requireNonNull(host);
        Objects.requireNonNull(record);
        Objects.requireNonNull(request);
        Objects.requireNonNull(secrets);
        WebClient.Builder restClient = WebClient.builder()
                .baseUrl(host.toString())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        secrets.getKeyForConnectionHost(record)
                .ifPresent(s -> restClient.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + s));


        ChatCompletionResponse response = restClient
                .build()
                .post()
                .uri(CHAT_COMPLETION_ENDPOINT)
                .bodyValue(request)
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
                .bodyToMono(ChatCompletionResponse.class)
                .block(Duration.ofSeconds(DEFAULT_LLM_TIMEOUT.getSeconds()));

        if (response == null) {
            throw new IllegalStateException("LLM provider returned an empty response body");
        }

        if (response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("LLM provider returned no choices");
        }
        if (PRINT_RESPONSE) System.out.println(response);

        return response;
    }



}
