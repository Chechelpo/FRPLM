package io.github.chechelpo.frplm.utils.integrations;

import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

import static io.github.chechelpo.frplm.config.Constants.DEFAULT_LLM_TIMEOUT;

final class OpenAICompatible {
    private static final Logger log = (Logger) LoggerFactory.getLogger("OpenAICompatible");
    private static final String CHAT_COMPLETION_ENDPOINT = "/api/v1/chat/completions";
    private static final String CHAT_COMPLETION_MODELS = "/v1/models";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient restClient;

    private static final int MAX_IN_MEMORY_SIZE = 100 * 1024 * 1024; // 16 MiB

    OpenAICompatible(String host) {
        this.restClient = WebClient.builder()
                .baseUrl(host)
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .codecs(configurer ->
                        configurer.defaultCodecs()
                                .maxInMemorySize(MAX_IN_MEMORY_SIZE)
                )
                .build();
    }

    public WebClient getRestClient(){
        return this.restClient;
    }

    public @NotNull Optional<ChatCompletionResponse> generateNonStreaming(ChatCompletionRequest request, String apiKey) {
        WebClient.RequestBodyUriSpec process = restClient.post();
        if (apiKey != null) process.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        try{
            return Optional.ofNullable(process
                    .uri(CHAT_COMPLETION_ENDPOINT)
                    .bodyValue(OBJECT_MAPPER.writeValueAsString(request))
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
                    .block(Duration.ofSeconds(DEFAULT_LLM_TIMEOUT.getSeconds()))
            );
        } catch (Exception e){
            log.error(e.getMessage());
            return Optional.empty();
        }
    }

    public ModelResponses getModels(String apiKey){
        var process = restClient.get();
        if (apiKey != null) process.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

        return process
                .uri(CHAT_COMPLETION_MODELS)
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
                .block(Duration.ofSeconds(DEFAULT_LLM_TIMEOUT.getSeconds()));
    }
}
