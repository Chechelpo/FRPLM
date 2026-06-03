package chechelpo.frplm.openai_compatible;

public record GenerationConfig(
            boolean streaming,
            boolean exclude_reasoning,
            ReasoningEffort reasoning_effort
    ){}
