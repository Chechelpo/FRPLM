package chechelpo.frplm.utils.integrations;

import chechelpo.frplm.domain.connection.llm.LLMBackend;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionResponse;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static chechelpo.frplm.config.Constants.DEFAULT_LLM_TIMEOUT;

final class OpenRouter implements LLMKnownHost{
    private final static String MODEL_ENDPOINT = "/api/v1/models";
    private final OpenAICompatible client;

    OpenRouter(){
        assert LLMBackend.OPEN_ROUTER.host != null;
        this.client = new OpenAICompatible(LLMBackend.OPEN_ROUTER.host.toString());
    }

    @Override
    public Optional<ChatCompletionResponse> generate(ChatCompletionRequest request, String apiKey) {
        return client.generateNonStreaming(request, apiKey);
    }

    private record OpenRouterModelsResponseDto(
            String object,
            List<OpenRouterModelDto> data
    ) {}
    private record OpenRouterModelDto(
            String id,
            String name,
            String description,
            Integer context_length,
            Long created,
            String canonical_slug,
            Object architecture,
            Object default_parameters,
            Object links,
            Object per_request_limits,
            Object pricing,
            List<String> supported_parameters,
            List<String> supported_voices,
            Object top_provider,
            Object benchmarks,
            String expiration_date,
            String hugging_face_id,
            String knowledge_cutoff,
            Object reasoning,
            Boolean supports_max_tokens
    ) {
        @Contract(" -> new")
        @NonNull ModelResponse toModelResponse() {
            return new ModelResponse(
                    this.id(),
                    "model",
                    this.name(),
                    this.context_length()
            );
        }
    }

    @Override
    public ModelResponses models(String apiKey) {
        WebClient.RequestHeadersSpec<?> process = client.getRestClient().get()
                .uri(MODEL_ENDPOINT);
        if (apiKey != null) process.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

        OpenRouterModelsResponseDto response = process
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
                                                        + errorBody )
                                ))
                )
                .bodyToMono(OpenRouterModelsResponseDto.class)
                .block(Duration.ofSeconds(DEFAULT_LLM_TIMEOUT.getSeconds()));

        if (response == null || response.data == null) {
            return new ModelResponses("list", new ModelResponse[0]);
        }

        ModelResponse[] data = response.data.stream()
                .map(OpenRouterModelDto::toModelResponse)
                .toArray(ModelResponse[]::new);

        return new ModelResponses(response.object(), data);
    }
// ... existing code ...


    @Override
    public Integer tokenize(String modelId, String text, String apiKey) {
        return 0;
    }
}
