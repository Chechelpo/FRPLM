package chechelpo.demo.llm.backends;

import chechelpo.demo.utils.llm.OpenAIEndpoints;
import chechelpo.demo.llm.legacy.ConnectionRegistry;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public final class KoboldAI implements LLM_service {
    public KoboldAI() {}

    @Override
    public @NotNull String[] getModels() {
        return new String[0];
    }

    private record TokenResponse(List<Integer> tokens) {}
    /**
     * @param raw string to tokenize
     * @return the amount of tokens in this string
     */
    @Override
    public long countTokens(String raw) {
        TokenResponse response = ConnectionRegistry.getWebClient().post()
                .uri(OpenAIEndpoints.TOKENIZER.getSuffix())
                .bodyValue(Map.of("input", raw))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block(); // blocking style

        if (response == null)
            return 0;

        return response.tokens().size();
    }


    private record MaxTokensResponse(long value) {}
    /**
     * @return the max amount of tokens configured in the kobold backend for loaded model
     */
    @Override
    public long getMaxTokens() {
        MaxTokensResponse response = ConnectionRegistry.getWebClient().get()
                .uri(OpenAIEndpoints.MAX_CONTENT_LENGTH.getSuffix())
                .retrieve()
                .bodyToMono(MaxTokensResponse.class)
                .block();

        if (response == null)
            return 0;

        return response.value();
    }


    private record GenerationRequest(
            String prompt,
            int max_length,
            float temperature,
            float top_p,
            float rep_pen
    ) {}
    private record GenerationResult(String text) {}
    private record GenerationResponse(List<GenerationResult> results) {}
    /**
     * Generates a continuation for the given prompt using the Kobold backend.
     *
     * @param prompt text to send to the model
     * @param maxLength maximum tokens to generate
     * @return generated text (model continuation)
     */
    @Override
    public String generate(String prompt, int maxLength, float temperature, float top_p, float rep_pen) {

        GenerationResponse response = ConnectionRegistry.getWebClient().post()
                .uri(OpenAIEndpoints.GENERATE.getSuffix())
                .bodyValue(new GenerationRequest(
                        prompt,
                        maxLength,
                        temperature,   // temperature
                        top_p,   // top_p
                        rep_pen    // repetition penalty
                ))
                .retrieve()
                .bodyToMono(GenerationResponse.class)
                .block();

        if (response == null || response.results == null || response.results.isEmpty())
            return "";

        return response.results.getFirst().text;
    }
}
