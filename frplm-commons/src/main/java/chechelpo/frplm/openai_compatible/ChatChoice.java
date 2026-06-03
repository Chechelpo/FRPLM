package chechelpo.frplm.openai_compatible;

public record ChatChoice(
            ChatCompletionMessage message,
            String finish_reason
    ) {}