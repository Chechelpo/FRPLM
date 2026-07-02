package chechelpo.frplm.utils.tokenizers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestTokenizerRegistry {

    private static final String SAMPLE_TEXT = """
            Hello, tokenizer.

            This is a simple test containing:
            - English text
            - Numbers: 123456
            - Punctuation: !@#$%%^&*()
            - Unicode: español, 日本語, العربية
            - Code: public static void main(String[] args) {}
            """;

    private final TokenizerRegistry registry = new TokenizerRegistry();

    @ParameterizedTest(name = "Tokenize using model: {0}")
    @ValueSource(strings = {
            "zai-org/GLM-5",
            "zai-org/GLM-5-Air",
            "glm-5-next",

            "deepseek-ai/DeepSeek-V4",
            "deepseek-ai/DeepSeek-V4-Pro",
            "deepseek-v4",

            "anthropic/claude-sonnet",
            "meta-llama/llama-3.3-70b-instruct",
            "mistralai/mistral-large",
            "google/gemma-3-27b-it",
            "ai21/jamba-large",
            "01-ai/yi-large",

            "openai/gpt-4o-mini",
            "openai/gpt-4-turbo",
            "gpt2"
    })
    void shouldLoadTokenizerAndCountTokens(String modelId) {
        Tokenizer tokenizer = registry.getForModel(modelId);

        int count = tokenizer.tokenCount("""
            Test text with English, español, 中文, numbers 123456,
            punctuation, and code: System.out.println("hello");
            """);

        assertNotNull(tokenizer);
        assertTrue(count > 0);

        System.out.printf(
                "%-45s -> %-35s -> %d tokens%n",
                modelId,
                tokenizer.getClass().getSimpleName(),
                count
        );
    }

    @Test
    void shouldCacheTokenizerInstances() {
        Tokenizer first = registry.getForModel(
                "meta-llama/llama-3.1-8b-instruct"
        );

        Tokenizer second = registry.getForModel(
                "meta-llama/llama-3.2-3b-instruct"
        );

        assertSame(
                first,
                second,
                "Models resolving to llama3 should share the cached tokenizer"
        );
    }

    @AfterAll
    void closeTokenizers() {
        registry.closeLoadedTokenizers();
    }
}