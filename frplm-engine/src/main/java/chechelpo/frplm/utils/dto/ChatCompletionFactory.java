package chechelpo.frplm.utils.dto;

import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import chechelpo.frplm.openai_compatible.GenerationConfig;
import chechelpo.frplm.openai_compatible.GenerationParameters;
import chechelpo.frplm.openai_compatible.ReasoningEffort;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ChatCompletionFactory {
    private ChatCompletionFactory() {}

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull GenerationParameters parametersFrom(@NotNull PromptTemplateRecord record) {
        return new GenerationParameters(
                record.getTemperature(),
                record.getTopP(),
                record.getFrequencyPenalty(),
                record.getPresencePenalty(),
                record.getRepetitionPenalty(),
                record.getTopK()
        );
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull GenerationConfig configFrom(@NotNull PromptTemplateRecord record) {
        return new GenerationConfig(
                record.getStreaming(),
                record.getExcludeReasoning(),
                ReasoningEffort.fromId(record.getReasoningEffort())
        );
    }
}
