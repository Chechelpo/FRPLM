package chechelpo.frplm.utils.prompts;

import chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public final class EntryEvaluator {
    private EntryEvaluator() {}

    public static boolean activates(
            EntryRecord entry,
            int recursionStep,
            int deepestKeyword
    ) {
        if (entry == null) return false;
        ActivationStrategy strategy = ActivationStrategy.of(entry.getStrategy());
        return switch (strategy) {
            case CONSTANT -> true;
            case COMMON -> evaluateCommonActivation(entry, recursionStep, deepestKeyword);
            case EMBEDDING -> evaluateCommonActivation(entry, recursionStep, deepestKeyword) || evaluateEmbeddingActivation();
            case null ->
                    throw new IllegalArgumentException("No activation strategy found for entry name " + entry.getName());
        };
    }

    private static boolean evaluateCommonActivation(
            EntryRecord entry,
            int recursionStep,
            int deepestKeyword
    ) {
        if (recursionStep != 0 && entry.getNonRecursable()) return false;
        if (entry.getScanDepth() != null && deepestKeyword > entry.getScanDepth()) return false;

        return evaluateProbability(entry);
    }
    private static boolean evaluateEmbeddingActivation(){
        return false;
    }

    private static boolean evaluateProbability(@NotNull EntryRecord entry) {
        if (entry.getProbability() == null) return true;
        int probability = entry.getProbability();

        if (probability < 0 || probability > 100) {
            throw new IllegalArgumentException("Probability must be between 0 and 100: " + probability);
        }

        int roll = ThreadLocalRandom.current().nextInt(1, 101); // 1..100 inclusive
        return roll <= probability;
    }
}
