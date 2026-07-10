package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

final class EntryEvaluator {
    private EntryEvaluator() {}
    enum EntryActivation {
        /** Keywords were not meant, entry may be recursed for */
        KEYWORDS_MISSING,
        /** No further recursion, entry failed after being eligible*/
        FAILED,
        /** No further recursion, entry is ready */
        SUCCESS
    }

    public static EntryActivation entryActivates(
            EntryRecord entry,
            Set<Integer> entryKeywords,
            int recursionStep,
            KeywordDetector detector
    ) {
        if (entry == null) return EntryActivation.FAILED;
        ActivationStrategy strategy = ActivationStrategy.of(entry.getStrategy());
        return switch (strategy) {
            case CONSTANT -> evaluateProbability(entry) ? EntryActivation.SUCCESS : EntryActivation.FAILED;
            case COMMON, EMBEDDING -> evaluateCommonActivation(entry, entryKeywords, recursionStep, detector);
            case null ->
                    throw new IllegalArgumentException("No activation strategy found for entry name " + entry.getName());
        };
    }

    private static EntryActivation evaluateCommonActivation(
            EntryRecord entry,
            Set<Integer> keywordIds,
            int recursionStep,
            KeywordDetector detector
    ) {
        if (recursionStep != 0 && entry.getNonRecursable())
            return EntryActivation.FAILED;
        if (!detector.containsKeywords(keywordIds)) return EntryActivation.KEYWORDS_MISSING;
        int deepestKeyword = getDeepestKeywordDepth(keywordIds, detector);
        if (entry.getScanDepth() != null && deepestKeyword > entry.getScanDepth())
            return EntryActivation.FAILED;

        return evaluateProbability(entry) ? EntryActivation.SUCCESS : EntryActivation.FAILED;
    }
    private static int getDeepestKeywordDepth(
            @NonNull Set<Integer> entryKeywords,
            @NonNull KeywordDetector keywordDetector
    ) {
        return entryKeywords.stream()
                .map(keywordDetector::getKeywordDetectionInfo)
                .flatMap(Optional::stream)
                .mapToInt(KeywordDetector.DetectedKeyword::atDepth)
                .max()
                .orElseThrow(() -> new RuntimeException("This function shouldn't be called if there's no keyword detected"));
    }

    private static EntryActivation evaluateEmbeddingActivation(){
        return EntryActivation.FAILED;
    }

    private static boolean evaluateProbability(@NotNull EntryRecord entry) {
        if (entry.getProbability() == null || entry.getProbability() == 100) return true;
        int probability = entry.getProbability();

        if (probability < 0 || probability > 100) {
            throw new IllegalArgumentException("Probability must be between 0 and 100: " + probability);
        }

        int roll = ThreadLocalRandom.current().nextInt(1, 101); // 1..100 inclusive
        return roll <= probability;
    }



}
