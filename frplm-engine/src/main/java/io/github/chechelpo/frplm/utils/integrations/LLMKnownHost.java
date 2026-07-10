package io.github.chechelpo.frplm.utils.integrations;


import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRequest;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionResponse;

import java.util.Optional;

public interface LLMKnownHost {
    Optional<ChatCompletionResponse> generate(ChatCompletionRequest request, String apiKey);
    ModelResponses models(String apiKey);
    Integer tokenize(String modelId, String text, String apiKey);
}
