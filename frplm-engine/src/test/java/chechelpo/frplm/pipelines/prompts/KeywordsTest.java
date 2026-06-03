package chechelpo.frplm.pipelines.prompts;

import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class KeywordsTest {
    private static final Method COMPILE_PATTERN =
            method("compilePattern", String.class);

    // -----------------------------------------------------------------------------------------------------------------
    // detectedKeywords
    // -----------------------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("detectedKeywords")
    final class DetectedKeywordsTests {

        @Test
        void returnsEmptySetWhenNoMessagesAreProvided() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "alpha")),
                    List.of()
            );

            assertIntSetEquals(detected);
        }

        @Test
        void returnsEmptySetWhenNoKeywordsAreProvided() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(),
                    List.of(message("alpha beta gamma"))
            );

            assertIntSetEquals(detected);
        }

        @Test
        void ignoresNullBlankAndWhitespaceOnlyMessages() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "alpha")),
                    List.of(
                            message(null),
                            message(""),
                            message(" "),
                            message("\t"),
                            message("\r\n")
                    )
            );

            assertIntSetEquals(detected);
        }

        @Test
        void detectsSingleKeywordInSingleMessage() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "alpha")),
                    List.of(message("before alpha after"))
            );

            assertIntSetEquals(detected, 1);
        }

        @Test
        void detectsSeveralKeywordsInOneMessage() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(
                            keyword(1, "alpha"),
                            keyword(2, "beta"),
                            keyword(3, "gamma")
                    ),
                    List.of(message("alpha and gamma are present"))
            );

            assertIntSetEquals(detected, 1, 3);
        }

        @Test
        void detectsKeywordsAcrossSeveralMessages() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(
                            keyword(1, "alpha"),
                            keyword(2, "beta"),
                            keyword(3, "gamma")
                    ),
                    List.of(
                            message("first message has alpha"),
                            message("second message has beta"),
                            message("third has nothing")
                    )
            );

            assertIntSetEquals(detected, 1, 2);
        }

        @Test
        void doesNotDuplicateKeywordIdsWhenKeywordAppearsManyTimes() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "alpha")),
                    List.of(
                            message("alpha alpha alpha"),
                            message("alpha again")
                    )
            );

            assertEquals(1, detected.size());
            assertTrue(detected.contains(1));
        }

        @Test
        void duplicateKeywordDefinitionsWithSameIdStillProduceOneId() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(
                            keyword(1, "alpha"),
                            keyword(1, "beta")
                    ),
                    List.of(message("alpha beta"))
            );

            assertIntSetEquals(detected, 1);
        }

        @Test
        void duplicateKeywordNamesWithDifferentIdsProduceBothIds() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(
                            keyword(1, "alpha"),
                            keyword(2, "alpha")
                    ),
                    List.of(message("alpha"))
            );

            assertIntSetEquals(detected, 1, 2);
        }

        @Test
        void detectionIsCaseInsensitive() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(
                            keyword(1, "alpha"),
                            keyword(2, "BETA")
                    ),
                    List.of(message("ALPHA beta"))
            );

            assertIntSetEquals(detected, 1, 2);
        }

        @Test
        void detectionIsUnicodeCaseInsensitiveWhereJavaUnicodeCaseSupportsIt() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "café")),
                    List.of(message("CAFÉ"))
            );

            assertIntSetEquals(detected, 1);
        }

        @Test
        void doesNotMatchKeywordInsideLongerLetterToken() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "cat")),
                    List.of(
                            message("concatenate"),
                            message("wildcat"),
                            message("catfish")
                    )
            );

            assertIntSetEquals(detected);
        }

        @Test
        void doesNotMatchKeywordInsideLongerNumericToken() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "123")),
                    List.of(
                            message("x123"),
                            message("123x"),
                            message("91234")
                    )
            );

            assertIntSetEquals(detected);
        }

        @Test
        void matchesKeywordDelimitedByPunctuation() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "alpha")),
                    List.of(message("(alpha), [alpha]; /alpha/"))
            );

            assertIntSetEquals(detected, 1);
        }

        @Test
        void matchesKeywordDelimitedByWhitespace() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "alpha")),
                    List.of(message("\talpha\n"))
            );

            assertIntSetEquals(detected, 1);
        }

        @Test
        void matchesKeywordAtBeginningAndEndOfText() {
            assertIntSetEquals(
                    KeywordDetection.detectedKeywords(
                            keywords(keyword(1, "alpha")),
                            List.of(message("alpha"))
                    ),
                    1
            );

            assertIntSetEquals(
                    KeywordDetection.detectedKeywords(
                            keywords(keyword(1, "alpha")),
                            List.of(message("alpha then text"))
                    ),
                    1
            );

            assertIntSetEquals(
                    KeywordDetection.detectedKeywords(
                            keywords(keyword(1, "alpha")),
                            List.of(message("text then alpha"))
                    ),
                    1
            );
        }

        @Test
        void multiTokenKeywordMatchesAcrossPunctuation() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "alpha beta")),
                    List.of(
                            message("alpha beta"),
                            message("alpha-beta"),
                            message("alpha/beta"),
                            message("alpha, beta")
                    )
            );

            assertIntSetEquals(detected, 1);
        }

        @Test
        void multiTokenKeywordMatchesAcrossMultiplePunctuationCharacters() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "alpha beta")),
                    List.of(message("alpha---///... beta"))
            );

            assertIntSetEquals(detected, 1);
        }

        @Test
        void multiTokenKeywordMatchesAcrossNewlineInsideSameMessage() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "alpha beta")),
                    List.of(message("alpha\nbeta"))
            );

            assertIntSetEquals(detected, 1);
        }

        @Test
        void multiTokenKeywordDoesNotMatchAcrossSeparateMessages() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "alpha beta")),
                    List.of(
                            message("alpha"),
                            message("beta")
                    )
            );

            assertIntSetEquals(detected);
        }

        @Test
        void multiTokenKeywordDoesNotMatchWhenTokensAreSeparatedOnlyByLetters() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "alpha beta")),
                    List.of(message("alphabeta"))
            );

            assertIntSetEquals(detected);
        }

        @Test
        void punctuationInKeywordIsTreatedAsFlexibleDelimiter() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(
                            keyword(1, "foo-bar"),
                            keyword(2, "x.y"),
                            keyword(3, "a/b")
                    ),
                    List.of(message("foo bar x-y a___b"))
            );

            assertIntSetEquals(detected, 1, 2, 3);
        }

        @Test
        void leadingAndTrailingPunctuationInKeywordAreIgnoredByTokenization() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(
                            keyword(1, "...alpha..."),
                            keyword(2, "---beta---")
                    ),
                    List.of(message("alpha beta"))
            );

            assertIntSetEquals(detected, 1, 2);
        }

        @Test
        void leadingAndTrailingWhitespaceInKeywordAreIgnored() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "   alpha beta   ")),
                    List.of(message("alpha-beta"))
            );

            assertIntSetEquals(detected, 1);
        }

        @Test
        void numericAndAlphabeticTokensAreHandledTogether() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(
                            keyword(1, "GPT 5"),
                            keyword(2, "model 2026")
                    ),
                    List.of(message("gpt-5 and model/2026"))
            );

            assertIntSetEquals(detected, 1, 2);
        }

        @Test
        void unicodeLettersAreTreatedAsWordCharactersForBoundaries() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "niño")),
                    List.of(
                            message("el niño juega"),
                            message("xniñox")
                    )
            );

            assertIntSetEquals(detected, 1);
        }

        @Test
        void unicodeBoundaryPreventsSubstringMatchInsideUnicodeWord() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(keyword(1, "niño")),
                    List.of(message("xniñox"))
            );

            assertIntSetEquals(detected);
        }

        @Test
        void detectsKeywordAfterSkippingAlreadyFoundKeywordInSamePartialSet() {
            IntSet detected = KeywordDetection.detectedKeywords(
                    keywords(
                            keyword(1, "alpha"),
                            keyword(2, "beta")
                    ),
                    List.of(
                            message("alpha"),
                            message("alpha beta")
                    )
            );

            assertIntSetEquals(detected, 1, 2);
        }

        @Test
        void invalidKeywordCausesDetectedKeywordsToThrowBeforeScanningMessages() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> KeywordDetection.detectedKeywords(
                            keywords(keyword(1, "alpha"), keyword(2, " ")),
                            List.of(message("alpha"))
                    )
            );

            assertEquals("Keyword cannot be null or blank", exception.getMessage());
        }

        @Test
        void deterministicForLargeInputDespiteParallelStream() {
            IntObjectPair<String>[] keywords = keywords(
                    keyword(1, "alpha"),
                    keyword(2, "beta gamma"),
                    keyword(3, "delta"),
                    keyword(4, "epsilon 42")
            );

            List<MessagesRecord> messages = java.util.stream.IntStream.range(0, 1_000)
                    .mapToObj(index -> {
                        if (index % 4 == 0) {
                            return message("alpha");
                        }

                        if (index % 4 == 1) {
                            return message("beta-gamma");
                        }

                        if (index % 4 == 2) {
                            return message("delta");
                        }

                        return message("epsilon/42");
                    })
                    .toList();

            for (int i = 0; i < 25; i++) {
                assertIntSetEquals(
                        KeywordDetection.detectedKeywords(keywords, messages),
                        1, 2, 3, 4
                );
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // compiledKeywords
    // -----------------------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("compiledKeywords")
    final class CompiledKeywordsTests {

        @Test
        void returnsOneCompiledPatternPerKeyword() {
            IntObjectPair<Pattern>[] compiled = KeywordDetection.compiledKeywords(
                    keywords(
                            keyword(10, "alpha"),
                            keyword(20, "beta gamma")
                    )
            );

            assertEquals(2, compiled.length);

            assertEquals(10, compiled[0].firstInt());
            assertTrue(KeywordDetection.keywordDetected("alpha", compiled[0].second()));

            assertEquals(20, compiled[1].firstInt());
            assertTrue(KeywordDetection.keywordDetected("beta-gamma", compiled[1].second()));
        }

        @Test
        void preservesInputOrder() {
            IntObjectPair<Pattern>[] compiled = KeywordDetection.compiledKeywords(
                    keywords(
                            keyword(3, "c"),
                            keyword(1, "a"),
                            keyword(2, "b")
                    )
            );

            assertEquals(3, compiled[0].firstInt());
            assertEquals(1, compiled[1].firstInt());
            assertEquals(2, compiled[2].firstInt());
        }

        @Test
        void returnsEmptyArrayForEmptyInput() {
            IntObjectPair<Pattern>[] compiled = KeywordDetection.compiledKeywords(keywords());

            assertEquals(0, compiled.length);
        }

        @Test
        void throwsWhenKeywordNameIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> KeywordDetection.compiledKeywords(
                            keywords(keyword(1, null))
                    )
            );

            assertEquals("Keyword cannot be null or blank", exception.getMessage());
        }

        @Test
        void throwsWhenKeywordNameIsBlank() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> KeywordDetection.compiledKeywords(
                            keywords(keyword(1, "   "))
                    )
            );

            assertEquals("Keyword cannot be null or blank", exception.getMessage());
        }

        @Test
        void compiledPatternIsCaseInsensitive() {
            IntObjectPair<Pattern>[] compiled = KeywordDetection.compiledKeywords(
                    keywords(keyword(1, "alpha"))
            );

            assertTrue(KeywordDetection.keywordDetected("ALPHA", compiled[0].second()));
            assertTrue(KeywordDetection.keywordDetected("Alpha", compiled[0].second()));
            assertTrue(KeywordDetection.keywordDetected("alpha", compiled[0].second()));
        }

        @Test
        void compiledPatternIsPunctuationInsensitiveBetweenKeywordTokens() {
            IntObjectPair<Pattern>[] compiled = KeywordDetection.compiledKeywords(
                    keywords(keyword(1, "alpha beta gamma"))
            );

            Pattern pattern = compiled[0].second();

            assertTrue(KeywordDetection.keywordDetected("alpha beta gamma", pattern));
            assertTrue(KeywordDetection.keywordDetected("alpha-beta-gamma", pattern));
            assertTrue(KeywordDetection.keywordDetected("alpha/beta/gamma", pattern));
            assertTrue(KeywordDetection.keywordDetected("alpha...beta___gamma", pattern));
        }

        @Test
        void compiledPatternDoesNotTreatRegexCharactersAsRegexSyntax() {
            IntObjectPair<Pattern>[] compiled = KeywordDetection.compiledKeywords(
                    keywords(keyword(1, "a.b+c?"))
            );

            Pattern pattern = compiled[0].second();

            assertTrue(KeywordDetection.keywordDetected("a b c", pattern));
            assertTrue(KeywordDetection.keywordDetected("a.b+c?", pattern));

            assertFalse(KeywordDetection.keywordDetected("axbccc", pattern));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // private compilePattern
    // -----------------------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("private compilePattern")
    final class CompilePatternTests {

        @Test
        void rejectsNullKeyword() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> compilePattern(null)
            );

            assertEquals("Keyword cannot be null or blank", exception.getMessage());
        }

        @Test
        void rejectsEmptyKeyword() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> compilePattern("")
            );

            assertEquals("Keyword cannot be null or blank", exception.getMessage());
        }

        @Test
        void rejectsBlankKeyword() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> compilePattern(" \t\r\n ")
            );

            assertEquals("Keyword cannot be null or blank", exception.getMessage());
        }

        @Test
        void singleTokenPatternMatchesExactToken() {
            Pattern pattern = compilePattern("alpha");

            assertTrue(KeywordDetection.keywordDetected("alpha", pattern));
            assertTrue(KeywordDetection.keywordDetected("x alpha y", pattern));
            assertTrue(KeywordDetection.keywordDetected("(alpha)", pattern));
        }

        @Test
        void singleTokenPatternRejectsEmbeddedToken() {
            Pattern pattern = compilePattern("alpha");

            assertFalse(KeywordDetection.keywordDetected("xalpha", pattern));
            assertFalse(KeywordDetection.keywordDetected("alphax", pattern));
            assertFalse(KeywordDetection.keywordDetected("xalphax", pattern));
        }

        @Test
        void multiTokenPatternRequiresAllTokensInOrder() {
            Pattern pattern = compilePattern("alpha beta gamma");

            assertTrue(KeywordDetection.keywordDetected("alpha beta gamma", pattern));
            assertTrue(KeywordDetection.keywordDetected("alpha-beta-gamma", pattern));

            assertFalse(KeywordDetection.keywordDetected("alpha gamma beta", pattern));
            assertFalse(KeywordDetection.keywordDetected("alpha beta", pattern));
            assertFalse(KeywordDetection.keywordDetected("beta gamma", pattern));
        }

        @Test
        void multiTokenPatternRequiresNonAlphanumericSeparatorBetweenTokens() {
            Pattern pattern = compilePattern("alpha beta");

            assertTrue(KeywordDetection.keywordDetected("alpha beta", pattern));
            assertTrue(KeywordDetection.keywordDetected("alpha-beta", pattern));
            assertTrue(KeywordDetection.keywordDetected("alpha_beta", pattern));

            assertFalse(KeywordDetection.keywordDetected("alphabeta", pattern));
            assertFalse(KeywordDetection.keywordDetected("alpha1beta", pattern));
            assertFalse(KeywordDetection.keywordDetected("alphaXbeta", pattern));
        }

        @Test
        void punctuationOnlyKeywordCurrentlyCompilesToBoundaryOnlyPattern() {
            Pattern pattern = compilePattern("---");

            assertTrue(KeywordDetection.keywordDetected("", pattern));
            assertTrue(KeywordDetection.keywordDetected("abc", pattern));
        }

        @Test
        void trimsKeywordBeforeTokenization() {
            Pattern pattern = compilePattern("   alpha beta   ");

            assertTrue(KeywordDetection.keywordDetected("alpha-beta", pattern));
            assertFalse(KeywordDetection.keywordDetected("xalpha-beta", pattern));
        }

        @Test
        void unicodeLettersAndNumbersAreTokens() {
            Pattern pattern = compilePattern("niño 42 café");

            assertTrue(KeywordDetection.keywordDetected("NIÑO-42-CAFÉ", pattern));
            assertTrue(KeywordDetection.keywordDetected("niño/42/café", pattern));

            assertFalse(KeywordDetection.keywordDetected("xniño-42-café", pattern));
            assertFalse(KeywordDetection.keywordDetected("niño-42-caféx", pattern));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // keywordDetected
    // -----------------------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("keywordDetected")
    final class KeywordDetectedTests {

        @Test
        void returnsTrueWhenMatcherFindsAnyOccurrence() {
            Pattern pattern = Pattern.compile("beta");

            assertTrue(KeywordDetection.keywordDetected("alpha beta gamma", pattern));
        }

        @Test
        void returnsFalseWhenMatcherFindsNoOccurrence() {
            Pattern pattern = Pattern.compile("delta");

            assertFalse(KeywordDetection.keywordDetected("alpha beta gamma", pattern));
        }

        @Test
        void delegatesToPatternFindNotFullMatch() {
            Pattern pattern = Pattern.compile("beta");

            assertTrue(KeywordDetection.keywordDetected("alpha beta gamma", pattern));
            assertFalse(KeywordDetection.keywordDetected("alpha gamma", pattern));
        }

        @Test
        void respectsProvidedPatternFlags() {
            Pattern caseSensitive = Pattern.compile("alpha");
            Pattern caseInsensitive = Pattern.compile("alpha", Pattern.CASE_INSENSITIVE);

            assertFalse(KeywordDetection.keywordDetected("ALPHA", caseSensitive));
            assertTrue(KeywordDetection.keywordDetected("ALPHA", caseInsensitive));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Reflection wrappers
    // -----------------------------------------------------------------------------------------------------------------

    private static Pattern compilePattern(String keyword) {
        return invoke(COMPILE_PATTERN, keyword);
    }

    private static Method method(String name, Class<?>... parameterTypes) {
        try {
            Method method = KeywordDetection.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Missing method: " + name, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Method method, Object... arguments) {
        try {
            return (T) method.invoke(null, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw new AssertionError("Unexpected checked exception from reflected method", cause);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Reflection failed", exception);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Fixtures and assertions
    // -----------------------------------------------------------------------------------------------------------------

    private static MessagesRecord message(String content) {
        MessagesRecord record = new MessagesRecord();
        record.setContent(content);
        return record;
    }

    private static IntObjectPair<String> keyword(int id, String keyword) {
        return new IntObjectImmutablePair<>(id, keyword);
    }

    @SafeVarargs
    private static IntObjectPair<String>[] keywords(IntObjectPair<String>... keywords) {
        return keywords;
    }

    private static void assertIntSetEquals(IntSet actual, int... expectedIds) {
        assertEquals(
                expectedIds.length,
                actual.size(),
                "Unexpected IntSet size"
        );

        for (int expectedId : expectedIds) {
            assertTrue(
                    actual.contains(expectedId),
                    "Expected keyword id was not detected: " + expectedId
            );
        }
    }
}
