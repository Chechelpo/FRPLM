package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.utils.collections.IntSetFactory;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import it.unimi.dsi.fastutil.ints.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Detects all possible relevant keywords inside a series of strings */
final class KeywordDetector {
    public enum KeywordSource {
        DESCRIPTION,
        PROMPT_SECTION,
        LOREBOOK,
        CHAT_HISTORY
    }

    public record DetectedKeyword(int atDepth, KeywordSource source){}
    /** Found keyword id -> {@link DetectedKeyword} (information where it was detected) */
    private Int2ObjectOpenHashMap<DetectedKeyword> detectedKeywords;

    /** (keywordId -> name) keywords of ALL lorebooks, regardless if they were found or not */
    private final List<IntObjectPair<String>> keywordNamesByIds;

    public KeywordDetector(PromptRenderer builder, IntSet lorebookIds, @NonNull EntryKeywordService entries) {
        Objects.requireNonNull(builder);
        Objects.requireNonNull(lorebookIds);
        Objects.requireNonNull(entries);

        this.keywordNamesByIds = entries.getKeywords(lorebookIds);
        detectKeywordsInChat(builder.getChatHistory());
    }

    public Int2ObjectOpenHashMap<DetectedKeyword> getDetectedKeywords(){
        return this.detectedKeywords;
    }

    public @NonNull Optional<DetectedKeyword> getKeywordDetectionInfo(int keywordId){
        return Optional.ofNullable(detectedKeywords.get(keywordId));
    }
    /** @return true if keywordIds != empty and all keywords are detected */
    public boolean containsKeywords(Set<Integer> keywordIds){
        return !keywordIds.isEmpty() && detectedKeywords.keySet().containsAll(keywordIds);
    }
    public int getNumberOfDetectedKeywords(){
        return detectedKeywords.size();
    }

    public void addContainedIn(EntryRecord entry){
        Objects.requireNonNull(entry, "Entry is null");
        if (entry.getContent() == null || entry.getContent().isBlank()) return;
        keywordNamesByIds
                .forEach(keywordPair -> {
                    if (detectedKeywords.containsKey(keywordPair.firstInt())) return;
                    int keywordId = keywordPair.firstInt();

                    if (keywordDetected(entry.getContent(), compilePattern(keywordPair.right())))
                        detectedKeywords.put(
                                keywordId,
                                new DetectedKeyword(0, KeywordSource.LOREBOOK)
                        );
                });
    }

    /** Called in first pass */
    void detectKeywordsInChat(@NonNull List<ChatMessage> messages){
        int messageCount = messages.size();
        this.detectedKeywords = keywordNamesByIds.parallelStream()
                .map(keywordPair -> {
                    int keywordId = keywordPair.firstInt();
                    Pattern keywordPattern = compilePattern(keywordPair.second());

                    for (int i = 0; i < messageCount; i++) {
                        String message = messages.get(i).content();

                        if (message == null) throw new IllegalArgumentException("Message " + i + " is null");
                        if (message.isEmpty()) continue;

                        if (keywordDetected(message, keywordPattern)) {
                            return IntObjectPair.of(
                                    keywordId,
                                    new DetectedKeyword(i, KeywordSource.CHAT_HISTORY)
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

    void detectKeywordsIn(@NonNull List<SectionManager> sections){}

    @Contract("null, _ -> fail; !null, null -> fail")
    public static @NonNull IntList detectKeywordsInChat(List<IntObjectPair<String>> keywords, String message){
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

    private static boolean isUnique(@NonNull List<IntObjectPair<String>> keywords) {
        IntSet seen = IntSetFactory.ofLength(keywords.size());
        for (IntObjectPair<String> keyword : keywords)
            if (seen.contains(keyword.firstInt())) return false;
            else seen.add(keyword.firstInt());
        return true;
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
