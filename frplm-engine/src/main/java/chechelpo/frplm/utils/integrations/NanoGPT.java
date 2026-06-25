package chechelpo.frplm.utils.integrations;

import chechelpo.frplm.domain.connection.llm.LLMBackend;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.openai_compatible.ChatCompletionResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static chechelpo.frplm.config.Constants.DEFAULT_LLM_TIMEOUT;

final class NanoGPT implements LLMKnownHost {
    private final static String MODEL_ENDPOINT = "/api/v1/models";
    private final OpenAICompatible openAIClient;

    public NanoGPT() {
        this.openAIClient = new OpenAICompatible(LLMBackend.NANO_GPT.host.toString());
    }

    @Override
    public Optional<ChatCompletionResponse> generate(ChatCompletionRequest request, String apiKey) {
        return openAIClient.generateNonStreaming(request, apiKey);
    }

    @Override
    public ModelResponses models(String apiKey) {
        var client = this.openAIClient.getRestClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(MODEL_ENDPOINT)
                        .queryParam("detailed", true)
                        .build()
                );
        if (apiKey != null) client.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

        return client.retrieve()
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


    @Override
    public Integer tokenize(String modelId, String text, String apiKey) {
        return null;
    }
}
