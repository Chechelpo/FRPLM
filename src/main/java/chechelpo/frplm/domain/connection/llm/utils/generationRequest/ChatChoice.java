package chechelpo.frplm.domain.connection.llm.utils.generationRequest;

public record ChatChoice(
            ChatMessage message,
            String finish_reason
    ) {}