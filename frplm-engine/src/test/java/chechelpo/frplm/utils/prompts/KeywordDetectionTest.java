package chechelpo.frplm.utils.prompts;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static chechelpo.frplm.utils.prompts.KeywordDetection.detectIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeywordDetectionTest {

    @Test
    void detectKeywords_In_throwsOnNullMessages() {
        int messageCount = 10;
        List<String> messages = new ArrayList<>(messageCount);
        for (int i = 0; i < messageCount; i++) messages.add("aa" + i);
        messages.add(null);
        IntObjectPair<String>[] keywords = new IntObjectPair[messageCount];
        for (int i = 0; i < messageCount; i++) keywords[i] = IntObjectPair.of(i, "bb" + i);
        assertThrows(
                IllegalArgumentException.class,
                () -> detectIn(keywords, messages)
        );
    }
    @Test
    void detectKeywordsIn_detectsKeywordAtFirstMatchingDepth() {
        IntObjectPair<String>[] keywords = new IntObjectPair[] {
                IntObjectPair.of(1, "hello world"),
                IntObjectPair.of(2, "java")
        };

        List<String> messages = List.of(
                "irrelevant",
                "HELLO, WORLD!",
                "java appears later"
        );

        var detected = detectIn(keywords, messages);

        assertEquals(2, detected.size());
        assertEquals(new KeywordDetection.DetectedKeyword(1, 1), detected.get(1));
        assertEquals(new KeywordDetection.DetectedKeyword(2, 2), detected.get(2));
    }
    @Test
    void detectKeywordsIn_doesNotMatchInsideLargerWords() {
        IntObjectPair<String>[] keywords = new IntObjectPair[] {
                IntObjectPair.of(1, "cat"),
                IntObjectPair.of(2, "dog")
        };

        List<String> messages = List.of(
                "concatenate catalog dogmatic",
                "a cat and a dog"
        );

        var detected = detectIn(keywords, messages);

        assertEquals(2, detected.size());
        assertEquals(new KeywordDetection.DetectedKeyword(1, 1), detected.get(1));
        assertEquals(new KeywordDetection.DetectedKeyword(2, 1), detected.get(2));
    }
    @Test
    void detectKeywordsIn_ignoresBlankMessages() {
        IntObjectPair<String>[] keywords = new IntObjectPair[] {
                IntObjectPair.of(7, "target")
        };

        List<String> messages = List.of(
                "   ",
                "\t\n",
                "found target here"
        );

        var detected = detectIn(keywords, messages);

        assertEquals(1, detected.size());
        assertEquals(new KeywordDetection.DetectedKeyword(7, 2), detected.get(7));
    }
}