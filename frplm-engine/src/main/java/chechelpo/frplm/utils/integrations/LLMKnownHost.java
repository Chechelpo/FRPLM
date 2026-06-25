package chechelpo.frplm.utils.integrations;

import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRequest;
import chechelpo.frplm.openai_compatible.ChatCompletionResponse;

import java.util.Optional;

public interface LLMKnownHost {
    Optional<ChatCompletionResponse> generate(ChatCompletionRequest request, String apiKey);
    ModelResponses models(String apiKey);
    Integer tokenize(String modelId, String text, String apiKey);
}
