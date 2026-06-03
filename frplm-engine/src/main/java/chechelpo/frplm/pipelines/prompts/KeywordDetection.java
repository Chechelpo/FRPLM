package chechelpo.frplm.pipelines.prompts;

import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * In charge of detecting keywords in messages.
 * <h> Possible optimizations: </h>
 * <li>
 *     <ul>Join all the messages into a string builder, then iterate over CharSequence (parallel per keyword instead of per message)
 *     Should be better, tested previously and had a 33% speedup due to constant factors.
 *     </ul>
 * </li>
 */
final class KeywordDetection {
    private KeywordDetection() {}

    public record DetectedKeyword(int keywordID, int atDepth) {}
    /**
     * Keyword detection function. Next iterations should also return at which depth the keyword was detected.
     * @param keywords to search for (keywordID -> keyword name)
     * @param messages messages to scan
     * @return the IDs of the detected keywords
     * @implNote splits per section.
     */
    @Contract(pure = true)
    public static IntSet detectedKeywords(
            IntObjectPair<String> @NotNull [] keywords,
            @NotNull List<MessagesRecord> messages
    ) {
        IntObjectPair<Pattern>[] compiledPatterns = compiledKeywords(keywords);

        return messages.parallelStream()
                .collect(
                        IntOpenHashSet::new,

                        (detected, message) -> {
                            String text = message.getContent();

                            if (text == null || text.isBlank()) {
                                return;
                            }

                            for (IntObjectPair<Pattern> keyword : compiledPatterns) {
                                int keywordId = keyword.firstInt();

                                if (!detected.contains(keywordId)
                                        && keywordDetected(text, keyword.second())) {
                                    detected.add(keywordId);
                                }
                            }
                        },

                        IntSet::addAll
                );
    }

    @Contract(pure = true)
    public static IntObjectPair<Pattern> @NotNull [] compiledKeywords(
            IntObjectPair<String> @NotNull [] keywords
    ) {
        return Arrays.stream(keywords)
                .map(pair -> new IntObjectImmutablePair<>(
                        pair.firstInt(),
                        compilePattern(pair.second())
                ))
                .toArray(IntObjectPair[]::new);
    }

    /**
     * @param keyword keyword to compile
     * @return regex pattern that is case-insensitive and punctuation-insensitive
     */
    @Contract(value = "_ -> new", pure = true)
    private static @NotNull Pattern compilePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Keyword cannot be null or blank");
        }

        String body = Arrays.stream(keyword.trim().split("[^\\p{L}\\p{N}]+"))
                .filter(token -> !token.isEmpty())
                .map(Pattern::quote)
                .collect(Collectors.joining("[^\\p{L}\\p{N}]+"));

        String regex =
                "(?<![\\p{L}\\p{N}])" +
                        body +
                        "(?![\\p{L}\\p{N}])";

        return Pattern.compile(
                regex,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
    }

    public static boolean keywordDetected(String text, @NotNull Pattern keyword) {
        return keyword.matcher(text).find();
    }
}
