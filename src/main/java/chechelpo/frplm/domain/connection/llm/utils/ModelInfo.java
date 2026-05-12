package chechelpo.frplm.domain.connection.llm.utils;

import jakarta.annotation.Nullable;
/** Common interface for models. Specific model retrieval may vary.*/
public sealed interface ModelInfo permits NanoGPT.NanoGPTModelInfo {
    String modelID();

    default @Nullable String displayName() {
        return null;
    }

    default @Nullable String description() {
        return null;
    }

    default @Nullable Integer contextLength() {
        return null;
    }

    default @Nullable Integer maxOutputTokens() {
        return null;
    }

    default boolean supportsTextGeneration() {
        return true;
    }

    default boolean supportsVision() {
        return false;
    }

    default boolean supportsToolCalling() {
        return false;
    }

    default boolean supportsStructuredOutput() {
        return false;
    }

    default boolean supportsReasoning() {
        return false;
    }

    default @Nullable String category() {
        return null;
    }
}
