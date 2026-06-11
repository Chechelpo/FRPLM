package chechelpo.frplm.utils.prompts;

import chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * In charge of detecting keywords in messages.
 * <h> Possible optimizations: </h>
 * <li>
 *     <ul>Join all the messages into a string builder, then iterate over CharSequence (parallel per keyword instead of per message).
 *     Could be better that way
 *     </ul>
 * </li>
 */
public final class KeywordDetection {
    private KeywordDetection() {}

    public record DetectedKeyword(int keywordID, int atDepth) {}
    /**
     * Keyword detection function.
     * @param keywords to search for (keywordID -> keyword name). MUST BE UNIQUE
     * @param messages messages to scan
     * @return keyword ID -> {@link DetectedKeyword}
     * @implNote splits per keyword.
     */
    public static Int2ObjectMap<DetectedKeyword> detectParallelIn(
            List<IntObjectPair<String>> keywords,
            List<String> messages
    ) {
        if (keywords == null || messages == null) throw new IllegalArgumentException("keywords or messages cannot be null");
        assert isUnique(keywords) : "Keywords are not unique";

        int messageCount = messages.size();
        return keywords.parallelStream()
                .map(keywordPair -> {
                    int keywordId = keywordPair.firstInt();
                    Pattern keywordPattern = compilePattern(keywordPair.second());

                    for (int i = 0; i < messageCount; i++) {
                        String message = messages.get(i);

                        if (message == null) throw new IllegalArgumentException("Message " + i + " is null");
                        if (message.isEmpty()) continue;

                        if (keywordDetected(message, keywordPattern)) {
                            return IntObjectPair.of(
                                    keywordId,
                                    new DetectedKeyword(keywordId, i) // or include i
                            );
                        }
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(
                        Int2ObjectOpenHashMap::new,
                        (map, pair) -> map.put(pair.firstInt(), pair.second()),
                        Int2ObjectMap::putAll
                );
    }

    public static IntList detectKeywordsIn(List<IntObjectPair<String>> keywords, String message){
        if (keywords == null || message == null) throw new IllegalArgumentException("keywords or messages cannot be null");
        assert isUnique(keywords) : "Keywords are not unique";

        IntList matching = new IntArrayList(keywords.size()/2);
        keywords.forEach(keywordPair -> {
                    int keywordId = keywordPair.firstInt();
                    Pattern keywordPattern = compilePattern(keywordPair.second());
                    if (keywordDetected(message, keywordPattern)) matching.add(keywordId);
                });

        return matching;
    }

    private static boolean isUnique(List<IntObjectPair<String>> keywords) {
        IntSet seen = IntSetFactory.ofLength(keywords.size());
        for (IntObjectPair<String> keyword : keywords)
            if (seen.contains(keyword.firstInt())) return false;
            else seen.add(keyword.firstInt());
        return true;
    }

    @Contract(pure = true)
    private static IntObjectPair<Pattern> @NotNull [] compiledKeywords(
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

    private static boolean keywordDetected(String text, @NotNull Pattern keyword) {
        return keyword.matcher(text).find();
    }
}
