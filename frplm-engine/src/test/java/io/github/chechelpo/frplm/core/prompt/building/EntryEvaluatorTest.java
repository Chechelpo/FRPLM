package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static io.github.chechelpo.frplm.core.prompt.building.EntryEvaluator.EntryActivation.FAILED;
import static io.github.chechelpo.frplm.core.prompt.building.EntryEvaluator.EntryActivation.KEYWORDS_MISSING;
import static io.github.chechelpo.frplm.core.prompt.building.EntryEvaluator.EntryActivation.SUCCESS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntryEvaluatorTest {

    private static final DSLContext JOOQ =
            DSL.using(SQLDialect.DEFAULT);

    @Test
    void nullEntryFailsWithoutConsultingDetector() {
        KeywordDetector detector = mock(KeywordDetector.class);

        EntryEvaluator.EntryActivation result =
                EntryEvaluator.entryActivates(
                        null,
                        Set.of(1),
                        0,
                        detector
                );

        assertEquals(FAILED, result);
        verifyNoInteractions(detector);
    }

    @Test
    void unknownStrategyThrowsMeaningfulException() {
        KeywordDetector detector = mock(KeywordDetector.class);

        EntryRecord entry = entry(
                (short) 999,
                100,
                false,
                null
        );
        entry.setName("Unknown strategy entry");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EntryEvaluator.entryActivates(
                        entry,
                        Set.of(),
                        0,
                        detector
                )
        );

        assertEquals(
                "No activation strategy found for entry name Unknown strategy entry",
                exception.getMessage()
        );

        verifyNoInteractions(detector);
    }

    @Nested
    class ConstantActivation {

        @Test
        void activatesWhenProbabilityIsNull() {
            KeywordDetector detector = mock(KeywordDetector.class);

            EntryRecord entry = entry(
                    ActivationStrategy.CONSTANT,
                    null,
                    false,
                    null
            );

            EntryEvaluator.EntryActivation result =
                    EntryEvaluator.entryActivates(
                            entry,
                            Set.of(),
                            0,
                            detector
                    );

            assertEquals(SUCCESS, result);
            verifyNoInteractions(detector);
        }

        @Test
        void activatesWhenProbabilityIsOneHundred() {
            KeywordDetector detector = mock(KeywordDetector.class);

            EntryRecord entry = entry(
                    ActivationStrategy.CONSTANT,
                    100,
                    false,
                    null
            );

            EntryEvaluator.EntryActivation result =
                    EntryEvaluator.entryActivates(
                            entry,
                            Set.of(),
                            0,
                            detector
                    );

            assertEquals(SUCCESS, result);
            verifyNoInteractions(detector);
        }

        @Test
        void failsWhenProbabilityIsZero() {
            KeywordDetector detector = mock(KeywordDetector.class);

            EntryRecord entry = entry(
                    ActivationStrategy.CONSTANT,
                    0,
                    false,
                    null
            );

            EntryEvaluator.EntryActivation result =
                    EntryEvaluator.entryActivates(
                            entry,
                            Set.of(),
                            0,
                            detector
                    );

            assertEquals(FAILED, result);
            verifyNoInteractions(detector);
        }

        @Test
        void rejectsProbabilityBelowZero() {
            KeywordDetector detector = mock(KeywordDetector.class);

            EntryRecord entry = entry(
                    ActivationStrategy.CONSTANT,
                    -1,
                    false,
                    null
            );

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> EntryEvaluator.entryActivates(
                            entry,
                            Set.of(),
                            0,
                            detector
                    )
            );

            assertEquals(
                    "Probability must be between 0 and 100: -1",
                    exception.getMessage()
            );
        }

        @Test
        void rejectsProbabilityAboveOneHundred() {
            KeywordDetector detector = mock(KeywordDetector.class);

            EntryRecord entry = entry(
                    ActivationStrategy.CONSTANT,
                    101,
                    false,
                    null
            );

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> EntryEvaluator.entryActivates(
                            entry,
                            Set.of(),
                            0,
                            detector
                    )
            );

            assertEquals(
                    "Probability must be between 0 and 100: 101",
                    exception.getMessage()
            );
        }
    }

    @Nested
    class CommonActivation {

        @Test
        void nonRecursableEntryFailsDuringRecursion() {
            KeywordDetector detector = mock(KeywordDetector.class);

            EntryRecord entry = entry(
                    ActivationStrategy.COMMON,
                    100,
                    true,
                    null
            );

            EntryEvaluator.EntryActivation result =
                    EntryEvaluator.entryActivates(
                            entry,
                            Set.of(1),
                            1,
                            detector
                    );

            assertEquals(FAILED, result);
            verifyNoInteractions(detector);
        }

        @Test
        void nonRecursableEntryCanActivateOnInitialPass() {
            KeywordDetector detector = mock(KeywordDetector.class);

            Set<Integer> keywords = Set.of(1);

            EntryRecord entry = entry(
                    ActivationStrategy.COMMON,
                    100,
                    true,
                    null
            );

            when(detector.containsKeywords(keywords))
                    .thenReturn(true);

            when(detector.getKeywordDetectionInfo(1))
                    .thenReturn(detectedAtDepth(0));

            EntryEvaluator.EntryActivation result =
                    EntryEvaluator.entryActivates(
                            entry,
                            keywords,
                            0,
                            detector
                    );

            assertEquals(SUCCESS, result);
        }

        @Test
        void returnsKeywordsMissingWhenRequiredKeywordsAreNotDetected() {
            KeywordDetector detector = mock(KeywordDetector.class);

            Set<Integer> keywords = Set.of(10, 20);

            EntryRecord entry = entry(
                    ActivationStrategy.COMMON,
                    100,
                    false,
                    null
            );

            when(detector.containsKeywords(keywords))
                    .thenReturn(false);

            EntryEvaluator.EntryActivation result =
                    EntryEvaluator.entryActivates(
                            entry,
                            keywords,
                            0,
                            detector
                    );

            assertEquals(KEYWORDS_MISSING, result);

            verify(detector).containsKeywords(keywords);
            verifyNoMoreInteractions(detector);
        }

        @Test
        void failsWhenDeepestKeywordExceedsScanDepth() {
            KeywordDetector detector = mock(KeywordDetector.class);

            Set<Integer> keywords = Set.of(10, 20, 30);

            EntryRecord entry = entry(
                    ActivationStrategy.COMMON,
                    100,
                    false,
                    4
            );

            when(detector.containsKeywords(keywords))
                    .thenReturn(true);

            when(detector.getKeywordDetectionInfo(10))
                    .thenReturn(detectedAtDepth(1));

            when(detector.getKeywordDetectionInfo(20))
                    .thenReturn(detectedAtDepth(5));

            when(detector.getKeywordDetectionInfo(30))
                    .thenReturn(detectedAtDepth(3));

            EntryEvaluator.EntryActivation result =
                    EntryEvaluator.entryActivates(
                            entry,
                            keywords,
                            0,
                            detector
                    );

            assertEquals(FAILED, result);
        }

        @Test
        void activatesWhenDeepestKeywordEqualsScanDepth() {
            KeywordDetector detector = mock(KeywordDetector.class);

            Set<Integer> keywords = Set.of(10, 20);

            EntryRecord entry = entry(
                    ActivationStrategy.COMMON,
                    100,
                    false,
                    5
            );

            when(detector.containsKeywords(keywords))
                    .thenReturn(true);

            when(detector.getKeywordDetectionInfo(10))
                    .thenReturn(detectedAtDepth(2));

            when(detector.getKeywordDetectionInfo(20))
                    .thenReturn(detectedAtDepth(5));

            EntryEvaluator.EntryActivation result =
                    EntryEvaluator.entryActivates(
                            entry,
                            keywords,
                            0,
                            detector
                    );

            assertEquals(SUCCESS, result);
        }

        @Test
        void activatesWithoutScanDepthLimit() {
            KeywordDetector detector = mock(KeywordDetector.class);

            Set<Integer> keywords = Set.of(7);

            EntryRecord entry = entry(
                    ActivationStrategy.COMMON,
                    100,
                    false,
                    null
            );

            when(detector.containsKeywords(keywords))
                    .thenReturn(true);

            when(detector.getKeywordDetectionInfo(7))
                    .thenReturn(detectedAtDepth(500));

            EntryEvaluator.EntryActivation result =
                    EntryEvaluator.entryActivates(
                            entry,
                            keywords,
                            0,
                            detector
                    );

            assertEquals(SUCCESS, result);
        }

        @Test
        void failsProbabilityCheckAfterKeywordsPass() {
            KeywordDetector detector = mock(KeywordDetector.class);

            Set<Integer> keywords = Set.of(7);

            EntryRecord entry = entry(
                    ActivationStrategy.COMMON,
                    0,
                    false,
                    null
            );

            when(detector.containsKeywords(keywords))
                    .thenReturn(true);

            when(detector.getKeywordDetectionInfo(7))
                    .thenReturn(detectedAtDepth(0));

            EntryEvaluator.EntryActivation result =
                    EntryEvaluator.entryActivates(
                            entry,
                            keywords,
                            0,
                            detector
                    );

            assertEquals(FAILED, result);
        }

        @Test
        void throwsWhenDetectorClaimsKeywordsExistButProvidesNoDetectionInfo() {
            KeywordDetector detector = mock(KeywordDetector.class);

            Set<Integer> keywords = Set.of(7);

            EntryRecord entry = entry(
                    ActivationStrategy.COMMON,
                    100,
                    false,
                    null
            );

            when(detector.containsKeywords(keywords))
                    .thenReturn(true);

            when(detector.getKeywordDetectionInfo(7))
                    .thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> EntryEvaluator.entryActivates(
                            entry,
                            keywords,
                            0,
                            detector
                    )
            );

            assertEquals(
                    "This function shouldn't be called if there's no keyword detected",
                    exception.getMessage()
            );
        }
    }

    @Nested
    class EmbeddingActivation {

        @Test
        void currentlyUsesTheCommonKeywordActivationPath() {
            KeywordDetector detector = mock(KeywordDetector.class);

            Set<Integer> keywords = Set.of(42);

            EntryRecord entry = entry(
                    ActivationStrategy.EMBEDDING,
                    100,
                    false,
                    null
            );

            when(detector.containsKeywords(keywords))
                    .thenReturn(true);

            when(detector.getKeywordDetectionInfo(42))
                    .thenReturn(detectedAtDepth(0));

            EntryEvaluator.EntryActivation result =
                    EntryEvaluator.entryActivates(
                            entry,
                            keywords,
                            0,
                            detector
                    );

            assertEquals(SUCCESS, result);
        }
    }

    private static Optional<KeywordDetector.DetectedKeyword> detectedAtDepth(
            int depth
    ) {
        return Optional.of(
                new KeywordDetector.DetectedKeyword(
                        depth,
                        KeywordDetector.KeywordSource.CHAT_HISTORY
                )
        );
    }

    private static EntryRecord entry(
            ActivationStrategy strategy,
            Integer probability,
            boolean nonRecursable,
            Integer scanDepth
    ) {
        return entry(
                strategy.stable_id,
                probability,
                nonRecursable,
                scanDepth
        );
    }

    private static EntryRecord entry(
            short strategy,
            Integer probability,
            boolean nonRecursable,
            Integer scanDepth
    ) {
        EntryRecord entry = JOOQ.newRecord(ENTRY);

        entry.setName("Test entry");
        entry.setStrategy(strategy);
        entry.setProbability(probability == null ? null : probability.shortValue());
        entry.setNonRecursable(nonRecursable);
        entry.setScanDepth(scanDepth == null ? null : scanDepth.shortValue());

        return entry;
    }
}