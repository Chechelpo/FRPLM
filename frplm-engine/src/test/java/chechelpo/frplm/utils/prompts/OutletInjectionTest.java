package chechelpo.frplm.utils.prompts;

import chechelpo.frplm.extensions.api.utils.DetectedOutlet;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OutletInjectionTest {
    private static EntryRecord entry(String content) {
        EntryRecord record = new EntryRecord();
        record.setContent(content);
        return record;
    }

    private static Int2ObjectMap<List<EntryRecord>> entriesByOutlet(
            int outletId,
            EntryRecord... entries
    ) {
        Int2ObjectMap<List<EntryRecord>> map = new Int2ObjectOpenHashMap<>();
        map.put(outletId, List.of(entries));
        return map;
    }

    @Test
    void injectSingleOutlet() {
        String original = "Before {{world}} after";

        List<DetectedOutlet> outlets = List.of(
                new DetectedOutlet(10, 0, "Before ".length())
        );

        Int2ObjectMap<List<EntryRecord>> activeEntries =
                entriesByOutlet(10, entry("Injected lore"));

        String rendered = OutletInjection.inject(
                original,
                outlets,
                activeEntries
        );

        assertEquals("Before Injected lore after", rendered);
    }

    @Test
    void injectMultipleEntriesIntoSingleOutletSeparatedByNewline() {
        String original = "Before {{world}} after";

        List<DetectedOutlet> outlets = List.of(
                new DetectedOutlet(10, 0, "Before ".length())
        );

        Int2ObjectMap<List<EntryRecord>> activeEntries =
                entriesByOutlet(
                        10,
                        entry("Entry A"),
                        entry("Entry B"),
                        entry("Entry C")
                );

        String rendered = OutletInjection.inject(
                original,
                outlets,
                activeEntries
        );

        assertEquals("Before Entry A\nEntry B\nEntry C after", rendered);
    }

    @Test
    void injectMultipleOutletsInSameSectionWithoutOffsetCorruption() {
        String original = "A {{first}} B {{second}} C";

        List<DetectedOutlet> outlets = List.of(
                new DetectedOutlet(1, 0, "A ".length()),
                new DetectedOutlet(2, 0, "A {{first}} B ".length())
        );

        Int2ObjectMap<List<EntryRecord>> activeEntries =
                new Int2ObjectOpenHashMap<>();

        activeEntries.put(1, List.of(entry("ONE")));
        activeEntries.put(2, List.of(entry("TWO")));

        String rendered = OutletInjection.inject(
                original,
                outlets,
                activeEntries
        );

        assertEquals("A ONE B TWO C", rendered);
    }

    @Test
    void injectMultipleOutletsWhereFirstReplacementChangesLength() {
        String original = "A {{first}} B {{second}} C";

        List<DetectedOutlet> outlets = List.of(
                new DetectedOutlet(1, 0, "A ".length()),
                new DetectedOutlet(2, 0, "A {{first}} B ".length())
        );

        Int2ObjectMap<List<EntryRecord>> activeEntries =
                new Int2ObjectOpenHashMap<>();

        activeEntries.put(1, List.of(entry("A much longer replacement")));
        activeEntries.put(2, List.of(entry("X")));

        String rendered = OutletInjection.inject(
                original,
                outlets,
                activeEntries
        );

        assertEquals("A A much longer replacement B X C", rendered);
    }

    @Test
    void injectOutletOnSecondLine() {
        String original = """
                Line one
                Before {{world}} after
                Line three""";

        List<DetectedOutlet> outlets = List.of(
                new DetectedOutlet(10, 1, "Before ".length())
        );

        Int2ObjectMap<List<EntryRecord>> activeEntries =
                entriesByOutlet(10, entry("Injected"));

        String rendered = OutletInjection.inject(
                original,
                outlets,
                activeEntries
        );

        String expected = """
                Line one
                Before Injected after
                Line three""";

        assertEquals(expected, rendered);
    }

    @Test
    void injectOutletWithWindowsLineEndings() {
        String original = "Line one\r\nBefore {{world}} after\r\nLine three";

        List<DetectedOutlet> outlets = List.of(
                new DetectedOutlet(10, 1, "Before ".length())
        );

        Int2ObjectMap<List<EntryRecord>> activeEntries =
                entriesByOutlet(10, entry("Injected"));

        String rendered = OutletInjection.inject(
                original,
                outlets,
                activeEntries
        );

        assertEquals(
                "Line one\r\nBefore Injected after\r\nLine three",
                rendered
        );
    }

    @Test
    void doesNotInjectWhenOutletHasNoActiveEntries() {
        String original = "Before after";

        List<DetectedOutlet> outlets = List.of(
                new DetectedOutlet(10, 0, "Before ".length())
        );

        Int2ObjectMap<List<EntryRecord>> activeEntries =
                new Int2ObjectOpenHashMap<>();

        activeEntries.put(999, List.of(entry("Wrong outlet")));

        String rendered = OutletInjection.inject(
                original,
                outlets,
                activeEntries
        );

        assertEquals(original, rendered);
    }

    @Test
    void doesNotInjectWhenEntryListIsEmpty() {
        String original = "Before after";

        List<DetectedOutlet> outlets = List.of(
                new DetectedOutlet(10, 0, "Before ".length())
        );

        Int2ObjectMap<List<EntryRecord>> activeEntries =
                new Int2ObjectOpenHashMap<>();

        activeEntries.put(10, List.of());

        String rendered = OutletInjection.inject(
                original,
                outlets,
                activeEntries
        );

        assertEquals(original, rendered);
    }

    @Test
    void ignoresNullBlankAndWhitespaceOnlyEntryContent() {
        List<EntryRecord> entries = List.of(
                entry("A"),
                entry(null),
                entry(""),
                entry("   "),
                entry("B")
        );

        String rendered = OutletInjection.renderEntries(entries);

        assertEquals("A\nB", rendered);
    }

    @Test
    void doesNotInjectWhenDetectedOffsetDoesNotPointAtMacroStart() {
        String original = "Before after";

        List<DetectedOutlet> outlets = List.of(
                new DetectedOutlet(10, 0, 0)
        );

        Int2ObjectMap<List<EntryRecord>> activeEntries =
                entriesByOutlet(10, entry("Injected"));

        String rendered = OutletInjection.inject(
                original,
                outlets,
                activeEntries
        );

        assertEquals(original, rendered);
    }

    @Test
    void doesNotInjectMalformedMacroWithoutClosingBraces() {
        String original = "Before {{world after";

        List<DetectedOutlet> outlets = List.of(
                new DetectedOutlet(10, 0, "Before ".length())
        );

        Int2ObjectMap<List<EntryRecord>> activeEntries =
                entriesByOutlet(10, entry("Injected"));

        String rendered = OutletInjection.inject(
                original,
                outlets,
                activeEntries
        );

        assertEquals(original, rendered);
    }

    @Test
    void returnsOriginalWhenNoDetectedOutlets() {
        String original = "Before {{world}} after";

        String rendered = OutletInjection.inject(
                original,
                List.of(),
                entriesByOutlet(10, entry("Injected"))
        );

        assertSame(original, rendered);
    }

    @Test
    void returnsOriginalWhenNoActiveEntriesExist() {
        String original = "Before {{world}} after";

        List<DetectedOutlet> outlets = List.of(
                new DetectedOutlet(10, 0, "Before ".length())
        );

        String rendered = OutletInjection.inject(
                original,
                outlets,
                new Int2ObjectOpenHashMap<>()
        );

        assertSame(original, rendered);
    }
}