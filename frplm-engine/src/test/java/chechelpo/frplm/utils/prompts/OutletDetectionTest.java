package chechelpo.frplm.utils.prompts;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static chechelpo.frplm.utils.prompts.OutletDetection.getDetectedOutlets;
import static org.junit.jupiter.api.Assertions.*;

class OutletDetectionTest {

    @Test
    void outletDetection_throwsOnNullInput(){
        assertThrows(
                IllegalArgumentException.class,
                () -> getDetectedOutlets(null, "message")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> getDetectedOutlets(new IntObjectPair[0], null)
        );
    }

    @Test
    void getDetectedOutlets_returnsEmptyListOnEmptyMessage() {
        IntObjectPair<String>[] outlets = new IntObjectPair[] {
                IntObjectPair.of(1, "character_info")
        };

        List<DetectedOutlet> detected = getDetectedOutlets(outlets, "");

        assertTrue(detected.isEmpty());
    }

    @Test
    void getDetectedOutlets_detectsSingleOutletMacroInSingleLine() {
        IntObjectPair<String>[] outlets = new IntObjectPair[] {
                IntObjectPair.of(1, "character_info")
        };

        String message = "prefix {{character_info}} suffix";

        List<DetectedOutlet> detected = getDetectedOutlets(outlets, message);

        assertEquals(1, detected.size());
        assertEquals(new DetectedOutlet(1, 0, 7), detected.getFirst());
    }

    @Test
    void getDetectedOutlets_detectsOutletSegmentIndexWithLfNewlines() {
        IntObjectPair<String>[] outlets = new IntObjectPair[] {
                IntObjectPair.of(2, "location_info")
        };

        String message = "first line\nbefore {{location_info}} after\nthird line";

        List<DetectedOutlet> detected = getDetectedOutlets(outlets, message);

        assertEquals(1, detected.size());
        assertEquals(new DetectedOutlet(2, 1, 7), detected.getFirst());
    }

    @Test
    void getDetectedOutlets_detectsOutletSegmentIndexWithCrLfNewlines() {
        IntObjectPair<String>[] outlets = new IntObjectPair[] {
                IntObjectPair.of(2, "location_info")
        };

        String message = "first line\r\nbefore {{location_info}} after\r\nthird line";

        List<DetectedOutlet> detected = getDetectedOutlets(outlets, message);

        assertEquals(1, detected.size());
        assertEquals(new DetectedOutlet(2, 1, 7), detected.getFirst());
    }

    @Test
    void getDetectedOutlets_detectsMultipleOutletsInOutletDeclarationOrder() {
        IntObjectPair<String>[] outlets = new IntObjectPair[] {
                IntObjectPair.of(1, "character_info"),
                IntObjectPair.of(2, "location_info")
        };

        String message = "{{location_info}}\n{{character_info}}";

        List<DetectedOutlet> detected = getDetectedOutlets(outlets, message);

        assertEquals(2, detected.size());

        assertEquals(new DetectedOutlet(1, 1, 0), detected.get(0));
        assertEquals(new DetectedOutlet(2, 0, 0), detected.get(1));
    }

    @Test
    void getDetectedOutlets_isCaseInsensitive() {
        IntObjectPair<String>[] outlets = new IntObjectPair[] {
                IntObjectPair.of(3, "world_info")
        };

        String message = "abc {{WORLD_INFO}} xyz";

        List<DetectedOutlet> detected = getDetectedOutlets(outlets, message);

        assertEquals(1, detected.size());
        assertEquals(new DetectedOutlet(3, 0, 4), detected.getFirst());
    }

    @Test
    void getDetectedOutlets_doesNotMatchRawOutletNameWithoutMacroBraces() {
        IntObjectPair<String>[] outlets = new IntObjectPair[] {
                IntObjectPair.of(4, "lorebook")
        };

        String message = "this mentions lorebook but not as a macro";

        List<DetectedOutlet> detected = getDetectedOutlets(outlets, message);

        assertTrue(detected.isEmpty());
    }

    @Test
    void getDetectedOutlets_detectsOnlyFirstLocationPerOutlet() {
        IntObjectPair<String>[] outlets = new IntObjectPair[] {
                IntObjectPair.of(5, "chat_history")
        };

        String message = "{{chat_history}}\nlater {{chat_history}}";

        List<DetectedOutlet> detected = getDetectedOutlets(outlets, message);

        assertEquals(1, detected.size());
        assertEquals(new DetectedOutlet(5, 0, 0), detected.getFirst());
    }
}