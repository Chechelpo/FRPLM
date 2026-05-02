package chechelpo.frplm.utils.llm;

/**
 * KoboldAI custom endpoints
 */
public enum OpenAIEndpoints {
    TOKENIZER("/api/extra/tokencount"),
    CURRENT_MODEL("/api/v1/model"),
    GENERATE("/api/v1/generate"),
    MAX_CONTENT_LENGTH("/api/v1/config/max_context_length");

    private final String suffix;

    OpenAIEndpoints(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }
}
