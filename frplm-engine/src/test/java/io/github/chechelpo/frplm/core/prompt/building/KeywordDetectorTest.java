package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.extensions.api.session.ChatMessage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KeywordDetectorTest {

    @Test
    void constructorRejectsNullArguments() {
        PromptRenderer renderer = mock(PromptRenderer.class);
        EntryKeywordService service = mock(EntryKeywordService.class);
        IntSet lorebookIds = new IntOpenHashSet();

        assertThrows(
                NullPointerException.class,
                () -> new KeywordDetector(null, lorebookIds, service)
        );

        assertThrows(
                NullPointerException.class,
                () -> new KeywordDetector(renderer, null, service)
        );

        assertThrows(
                NullPointerException.class,
                () -> new KeywordDetector(renderer, lorebookIds, null)
        );
    }

    @Test
    void constructorFetchesKeywordsForGivenLorebookIdsAndScansChatHistory() {
        PromptRenderer renderer = mock(PromptRenderer.class);
        EntryKeywordService service = mock(EntryKeywordService.class);
        IntSet lorebookIds = new IntOpenHashSet(new int[]{10, 20});

        List<IntObjectPair<String>> keywords = List.of(
                keyword(1, "dragon"),
                keyword(2, "castle")
        );

        List<ChatMessage> messages = List.of(
                message("The black dragon has returned.")
        );

        when(service.getKeywords(same(lorebookIds))).thenReturn(keywords);
        when(renderer.getChatHistory()).thenReturn(messages);

        KeywordDetector detector = new KeywordDetector(renderer, lorebookIds, service);

        verify(service).getKeywords(same(lorebookIds));
        verify(renderer).getChatHistory();

        assertEquals(1, detector.getNumberOfDetectedKeywords());
        assertTrue(detector.getDetectedKeywords().containsKey(1));
        assertFalse(detector.getDetectedKeywords().containsKey(2));
    }
    @Test
    void constructorHandlesNoKeywords() {
        KeywordDetector detector = detector(
                List.of(),
                List.of(message("dragon castle wizard"))
        );

        assertEquals(0, detector.getNumberOfDetectedKeywords());
        assertTrue(detector.getDetectedKeywords().isEmpty());
    }

    @Test
    void detectsKeywordInChatHistoryWithCorrectSourceAndDepth() {
        KeywordDetector detector = detector(
                List.of(keyword(1, "dragon")),
                List.of(
                        message("Nothing relevant here."),
                        message("A dragon appears."),
                        message("Another dragon appears later.")
                )
        );

        Optional<KeywordDetector.DetectedKeyword> detection =
                detector.getKeywordDetectionInfo(1);

        assertTrue(detection.isPresent());
        assertEquals(1, detection.get().atDepth());
        assertEquals(
                KeywordDetector.KeywordSource.CHAT_HISTORY,
                detection.get().source()
        );
    }

    @Test
    void detectsMultipleKeywordsAcrossMessages() {
        KeywordDetector detector = detector(
                List.of(
                        keyword(1, "dragon"),
                        keyword(2, "castle"),
                        keyword(3, "wizard")
                ),
                List.of(
                        message("The wizard speaks."),
                        message("The castle contains a dragon.")
                )
        );

        assertEquals(3, detector.getNumberOfDetectedKeywords());

        assertEquals(
                new KeywordDetector.DetectedKeyword(
                        1,
                        KeywordDetector.KeywordSource.CHAT_HISTORY
                ),
                detector.getKeywordDetectionInfo(1).orElseThrow()
        );

        assertEquals(
                new KeywordDetector.DetectedKeyword(
                        1,
                        KeywordDetector.KeywordSource.CHAT_HISTORY
                ),
                detector.getKeywordDetectionInfo(2).orElseThrow()
        );

        assertEquals(
                new KeywordDetector.DetectedKeyword(
                        0,
                        KeywordDetector.KeywordSource.CHAT_HISTORY
                ),
                detector.getKeywordDetectionInfo(3).orElseThrow()
        );
    }

    @Test
    void skipsEmptyMessagesButStillCountsDepthByOriginalMessageIndex() {
        KeywordDetector detector = detector(
                List.of(keyword(1, "dragon")),
                List.of(
                        message(""),
                        message(""),
                        message("The dragon is present.")
                )
        );

        KeywordDetector.DetectedKeyword detection =
                detector.getKeywordDetectionInfo(1).orElseThrow();

        assertEquals(2, detection.atDepth());
        assertEquals(KeywordDetector.KeywordSource.CHAT_HISTORY, detection.source());
    }

    @Test
    void throwsWhenChatMessageContentIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> detector(
                        List.of(keyword(1, "dragon")),
                        List.of(message(null))
                )
        );

        assertTrue(exception.getMessage().contains("Message 0"));
        assertTrue(exception.getMessage().contains("null"));
    }

    @Test
    void getKeywordDetectionInfoReturnsEmptyForUnknownKeyword() {
        KeywordDetector detector = detector(
                List.of(keyword(1, "dragon")),
                List.of(message("The dragon is present."))
        );

        assertTrue(detector.getKeywordDetectionInfo(999).isEmpty());
    }

    @Test
    void containsKeywordsReturnsWhetherAllRequestedKeywordsWereDetected() {
        KeywordDetector detector = detector(
                List.of(
                        keyword(1, "dragon"),
                        keyword(2, "castle"),
                        keyword(3, "wizard")
                ),
                List.of(message("The dragon is inside the castle."))
        );

        assertTrue(detector.containsKeywords(Set.of(1)));
        assertTrue(detector.containsKeywords(Set.of(1, 2)));
        assertFalse(detector.containsKeywords(Set.of(1, 3)));
        assertFalse(detector.containsKeywords(Set.of(999)));
    }

    @Test
    void addContainedInRejectsNullEntry() {
        KeywordDetector detector = detector(
                List.of(keyword(1, "dragon")),
                List.of()
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> detector.addContainedIn(null)
        );

        assertEquals("Entry is null", exception.getMessage());
    }

    @Test
    void addContainedInIgnoresNullOrBlankContent() {
        KeywordDetector detector = detector(
                List.of(keyword(1, "dragon")),
                List.of()
        );

        EntryRecord nullContentEntry = mock(EntryRecord.class);
        when(nullContentEntry.getContent()).thenReturn(null);

        EntryRecord blankContentEntry = mock(EntryRecord.class);
        when(blankContentEntry.getContent()).thenReturn("   \n\t   ");

        detector.addContainedIn(nullContentEntry);
        detector.addContainedIn(blankContentEntry);

        assertEquals(0, detector.getNumberOfDetectedKeywords());
    }

    @Test
    void addContainedInDetectsLorebookKeywordThatWasNotDetectedInChatHistory() {
        KeywordDetector detector = detector(
                List.of(
                        keyword(1, "dragon"),
                        keyword(2, "ancient key")
                ),
                List.of(message("The dragon was already mentioned."))
        );

        EntryRecord entry = mock(EntryRecord.class);
        when(entry.getContent()).thenReturn(
                "This lorebook entry contains the ancient-key."
        );

        detector.addContainedIn(entry);

        assertEquals(2, detector.getNumberOfDetectedKeywords());

        assertEquals(
                new KeywordDetector.DetectedKeyword(
                        0,
                        KeywordDetector.KeywordSource.CHAT_HISTORY
                ),
                detector.getKeywordDetectionInfo(1).orElseThrow()
        );

        assertEquals(
                new KeywordDetector.DetectedKeyword(
                        0,
                        KeywordDetector.KeywordSource.LOREBOOK
                ),
                detector.getKeywordDetectionInfo(2).orElseThrow()
        );
    }

    @Test
    void addContainedInDoesNotOverwriteExistingChatHistoryDetection() {
        KeywordDetector detector = detector(
                List.of(keyword(1, "dragon")),
                List.of(message("The dragon appears in chat history."))
        );

        EntryRecord entry = mock(EntryRecord.class);
        when(entry.getContent()).thenReturn("The dragon also appears in lorebook content.");

        detector.addContainedIn(entry);

        KeywordDetector.DetectedKeyword detection =
                detector.getKeywordDetectionInfo(1).orElseThrow();

        assertEquals(0, detection.atDepth());
        assertEquals(KeywordDetector.KeywordSource.CHAT_HISTORY, detection.source());
    }

    @Test
    void staticDetectionRejectsNullArguments() {
        IllegalArgumentException nullKeywords = assertThrows(
                IllegalArgumentException.class,
                () -> KeywordDetector.detectKeywordsInChat(null, "message")
        );

        IllegalArgumentException nullMessage = assertThrows(
                IllegalArgumentException.class,
                () -> KeywordDetector.detectKeywordsInChat(List.of(keyword(1, "dragon")), null)
        );

        assertTrue(nullKeywords.getMessage().contains("keywords or messages cannot be null"));
        assertTrue(nullMessage.getMessage().contains("keywords or messages cannot be null"));
    }

    @Test
    void staticDetectionRejectsNullKeywordText() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> KeywordDetector.detectKeywordsInChat(
                        List.of(IntObjectPair.of(1, null)),
                        "message"
                )
        );

        assertEquals("Keyword cannot be null or blank", exception.getMessage());
    }

    @Test
    void staticDetectionRejectsBlankKeywordText() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> KeywordDetector.detectKeywordsInChat(
                        List.of(keyword(1, "   \n\t   ")),
                        "message"
                )
        );

        assertEquals("Keyword cannot be null or blank", exception.getMessage());
    }

    @Test
    void staticDetectionReturnsEmptyListWhenThereAreNoMatches() {
        IntList matches = KeywordDetector.detectKeywordsInChat(
                List.of(
                        keyword(1, "dragon"),
                        keyword(2, "castle")
                ),
                "Only a wizard is present."
        );

        assertEquals(IntArrayList.of(), matches);
    }

    @Test
    void staticDetectionIsCaseInsensitive() {
        IntList matches = KeywordDetector.detectKeywordsInChat(
                List.of(keyword(1, "Dragon")),
                "A DRAGON appears."
        );

        assertEquals(IntArrayList.of(1), matches);
    }

    @Test
    void staticDetectionIsUnicodeCaseInsensitive() {
        IntList matches = KeywordDetector.detectKeywordsInChat(
                List.of(
                        keyword(1, "café"),
                        keyword(2, "ångström")
                ),
                "The CAFÉ measured one ÅNGSTRÖM."
        );

        assertEquals(IntArrayList.of(1, 2), matches);
    }

    @Test
    void staticDetectionDoesNotPerformAccentFolding() {
        IntList matches = KeywordDetector.detectKeywordsInChat(
                List.of(
                        keyword(1, "café"),
                        keyword(2, "ångström")
                ),
                "The cafe measured one angstrom."
        );

        assertTrue(matches.isEmpty());
    }

    @Test
    void staticDetectionIsPunctuationInsensitiveBetweenKeywordTokens() {
        IntList matches = KeywordDetector.detectKeywordsInChat(
                List.of(
                        keyword(1, "red dragon"),
                        keyword(2, "silver-key"),
                        keyword(3, "ancient_key")
                ),
                "The RED---DRAGON carried a silver key and an ancient/key."
        );

        assertEquals(IntArrayList.of(1, 2, 3), matches);
    }

    @Test
    void staticDetectionRequiresSeparatorsBetweenMultiTokenKeywordParts() {
        IntList matches = KeywordDetector.detectKeywordsInChat(
                List.of(keyword(1, "red dragon")),
                "The reddragon is not separated."
        );

        assertTrue(matches.isEmpty());
    }

    @Test
    void staticDetectionDoesNotMatchInsideLargerLetterOrNumberTokens() {
        IntList matches = KeywordDetector.detectKeywordsInChat(
                List.of(keyword(1, "cat")),
                "bobcat catfish scatter cat1 1cat concatenate"
        );

        assertTrue(matches.isEmpty());
    }

    @Test
    void staticDetectionMatchesWhenKeywordIsDelimitedByNonLettersAndNonNumbers() {
        IntList matches = KeywordDetector.detectKeywordsInChat(
                List.of(keyword(1, "cat")),
                "A dog, a cat, and a bird."
        );

        assertEquals(IntArrayList.of(1), matches);
    }

    @Test
    void staticDetectionTreatsUnderscoreAsDelimiter() {
        IntList matches = KeywordDetector.detectKeywordsInChat(
                List.of(keyword(1, "cat")),
                "black_cat_white"
        );

        assertEquals(IntArrayList.of(1), matches);
    }

    @Test
    void staticDetectionReturnsMatchesInKeywordListOrder() {
        IntList matches = KeywordDetector.detectKeywordsInChat(
                List.of(
                        keyword(3, "wizard"),
                        keyword(1, "dragon"),
                        keyword(2, "castle")
                ),
                "The dragon, castle, and wizard are all present."
        );

        assertEquals(IntArrayList.of(3, 1, 2), matches);
    }

    @Test
    void staticDetectionRejectsDuplicateKeywordIdsWhenAssertionsAreEnabled() {
        assumeTrue(
                KeywordDetector.class.desiredAssertionStatus(),
                "Enable assertions for this test with -ea."
        );

        AssertionError error = assertThrows(
                AssertionError.class,
                () -> KeywordDetector.detectKeywordsInChat(
                        List.of(
                                keyword(1, "dragon"),
                                keyword(1, "castle")
                        ),
                        "dragon castle"
                )
        );

        assertEquals("Keywords are not unique", error.getMessage());
    }

    @Disabled("""
            Current implementation accepts keywords such as '!!!' because tokenization
            produces an empty regex body. Enable this test after compilePattern rejects
            keywords with no Unicode letters or numbers.
            """)
    @Test
    void staticDetectionShouldRejectKeywordWithoutLetterOrNumberTokens() {
        assertThrows(
                IllegalArgumentException.class,
                () -> KeywordDetector.detectKeywordsInChat(
                        List.of(keyword(1, "!!!")),
                        "message"
                )
        );
    }

    private static KeywordDetector detector(
            List<IntObjectPair<String>> keywords,
            List<ChatMessage> messages
    ) {
        PromptRenderer renderer = mock(PromptRenderer.class);
        EntryKeywordService service = mock(EntryKeywordService.class);

        when(service.getKeywords(any(IntSet.class))).thenReturn(keywords);
        when(renderer.getChatHistory()).thenReturn(messages);

        return new KeywordDetector(
                renderer,
                new IntOpenHashSet(new int[]{100, 200}),
                service
        );
    }

    private static IntObjectPair<String> keyword(int id, String value) {
        return IntObjectPair.of(id, value);
    }

    private static ChatMessage message(String content) {
        ChatMessage message = mock(ChatMessage.class);
        when(message.content()).thenReturn(content);
        return message;
    }
}