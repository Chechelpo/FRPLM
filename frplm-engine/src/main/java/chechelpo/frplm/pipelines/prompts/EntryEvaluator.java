package chechelpo.frplm.pipelines.prompts;

import chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;

import static chechelpo.frplm.pipelines.prompts.PromptEntryPoint.PROMPT_LOGGER;


public final class EntryEvaluator {
    private EntryEvaluator() {}

    static @NotNull String renderEntries(
            @NotNull List<EntryRecord> entries,
            PromptRenderContext renderContext,
            byte[][] embeddings
    ) {
        PROMPT_LOGGER.debug("Rendering entries \n {}", entries);
        StringJoiner rendered = new StringJoiner("\n");

        for (EntryRecord entry : entries) {
            String content = entry.getContent();

            if (content != null && !content.isBlank() && EntryEvaluator.entryActivates(entry)) {
                rendered.add(content);
            }
        }

        return rendered.toString();
    }

    @CheckReturnValue
    private static boolean entryActivates(@NotNull EntryRecord entry) {
        ActivationStrategy strategy = ActivationStrategy.of(entry.getStrategy());
        boolean activates = switch (strategy) {
            case CONSTANT -> true;
            case COMMON -> evaluateCommonActivation(entry);
            case EMBEDDING -> evaluateCommonActivation(entry) || evaluateEmbeddingActivation(entry);
            case null -> throw new IllegalArgumentException("No activation strategy found for entry name " + entry.getName());
        };
        PROMPT_LOGGER.trace("Evaluating entry: {} result: {}", entry, activates);
        return activates;
    }

    private static boolean evaluateCommonActivation(@NotNull EntryRecord entry) {
        if (entry.getProbability() == null) return true;
        int probability = entry.getProbability();

        if (probability < 0 || probability > 100) {
            throw new IllegalArgumentException("Probability must be between 0 and 100: " + probability);
        }

        int roll = ThreadLocalRandom.current().nextInt(1, 101); // 1..100 inclusive
        return roll <= probability;
    }
    private static boolean evaluateEmbeddingActivation(@NotNull EntryRecord entry) {
        return false;
    }
}
