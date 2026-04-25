package chechelpo.demo.llm.backends;

import org.jetbrains.annotations.NotNull;

public sealed interface LLM_service permits KoboldAI {
    /** Model list of endpoint */
    @NotNull String[] getModels();

    long getMaxTokens();
    long countTokens(String raw);

    String generate(String prompt, int maxLength, float temperature, float top_p, float rep_pen);
}
