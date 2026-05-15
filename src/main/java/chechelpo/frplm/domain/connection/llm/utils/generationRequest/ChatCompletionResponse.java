package chechelpo.frplm.domain.connection.llm.utils.generationRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatCompletionResponse(
            List<ChatChoice> choices
    ) {}