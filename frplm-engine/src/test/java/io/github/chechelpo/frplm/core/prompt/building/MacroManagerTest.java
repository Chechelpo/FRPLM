package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.utils.matching.FlexiblePattern;
import io.github.chechelpo.frplm.utils.matching.Macro;
import io.github.chechelpo.frplm.utils.matching.Outlet;
import io.github.chechelpo.frplm.utils.matching.ReplacementTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MacroManagerTest {

    private OutletManagerImpl outletManager;
    private MacroManager macroManager;

    @BeforeEach
    void setUp() {
        outletManager = mock(OutletManagerImpl.class);
        macroManager = new MacroManager(outletManager);
    }

    @Test
    void testEntryAddition() {
        String outletName = "default";

        int testAmount = 10;
        List<EntryRecord> mockEntries = new ArrayList<>(testAmount);

        for (int i = 0; i < testAmount; i++) {
            EntryRecord entryRecord = new EntryRecord();
            entryRecord.setContent("Content" + i);
            mockEntries.add(entryRecord);
        }

        when(outletManager.getOutletOf(any(EntryRecord.class)))
                .thenReturn(new Outlet(outletName));

        macroManager.addEntries(mockEntries);

        assertEquals(
                mockEntries.stream()
                        .map(EntryRecord::getContent)
                        .toList(),
                macroManager.getAtMacro(new Outlet(outletName))
        );
    }

    @Test
    void normalizesMacros() {
        int amount = 10;

        Set<String> expectedMacroNames = new HashSet<>();

        for (int i = 0; i < amount; i++) {
            expectedMacroNames.add("macro " + i);
        }

        expectedMacroNames.forEach(
                macro -> macroManager.appendAtMacro(macro, "ignored")
        );

        Set<ReplacementTarget> expectedTargets = expectedMacroNames.stream()
                .map(Macro::new)
                .collect(Collectors.toSet());

        Set<ReplacementTarget> actualTargets =
                macroManager.getTargets();

        assertEquals(expectedTargets, actualTargets);
    }

    @Test
    void testAtIndex() {
        String macro = "macro";

        int testAmount = 10;
        List<String> expectedContents = new ArrayList<>(testAmount);

        for (int i = 0; i < testAmount; i++) {
            expectedContents.add("Content " + i);
        }

        expectedContents.forEach(
                content -> macroManager.appendAtMacro(macro, content)
        );

        String inserted = "inserted";

        macroManager.injectAtMacro(macro, inserted, 2);
        macroManager.injectAtMacro(macro, inserted, 4);

        List<String> actual = macroManager.getAtMacro(macro);

        assertEquals(inserted, actual.get(2));
        assertEquals(inserted, actual.get(4));
    }

    @Test
    void injectAtIndex_supportsHighIndexes() {
        String macro = "macro";

        int testAmount = 10;
        List<String> expectedContents = new ArrayList<>(testAmount);

        for (int i = 0; i < testAmount; i++) {
            expectedContents.add("Content " + i);
        }

        expectedContents.forEach(
                content -> macroManager.appendAtMacro(macro, content)
        );

        String inserted = "inserted";

        macroManager.injectAtMacro(
                macro,
                inserted,
                testAmount + 3
        );

        assertEquals(
                inserted,
                macroManager.getAtMacro(macro).get(testAmount)
        );
    }

    @Test
    void injectAtIndex_supportsNegativeIndexes() {
        String macro = "macro";

        int testAmount = 10;
        List<String> expectedContents = new ArrayList<>(testAmount);

        for (int i = 0; i < testAmount; i++) {
            expectedContents.add("Content " + i);
        }

        expectedContents.forEach(
                content -> macroManager.appendAtMacro(macro, content)
        );

        String inserted = "inserted";

        macroManager.injectAtMacro(
                macro,
                inserted,
                -2
        );

        assertEquals(
                inserted,
                macroManager.getAtMacro(macro).get(testAmount - 2)
        );
    }

    @Test
    void injectAtIndex_minusOneAppendsOnce() {
        String macro = "macro";

        macroManager.appendAtMacro(macro, "first");
        macroManager.injectAtMacro(macro, "second", -1);

        assertEquals(
                List.of("first", "second"),
                macroManager.getAtMacro(macro)
        );
    }

    @Test
    void testAppend() {
        String macro = "macro";

        int testAmount = 10;
        List<String> expectedContents = new ArrayList<>(testAmount);

        for (int i = 0; i < testAmount; i++) {
            expectedContents.add("Content " + i);
        }

        expectedContents.forEach(
                content -> macroManager.appendAtMacro(macro, content)
        );

        assertNotNull(macroManager.getAtMacro(macro));
        assertEquals(
                expectedContents,
                macroManager.getAtMacro(macro)
        );
    }

    @Test
    void testPrepend() {
        String macro = "macro";

        int testAmount = 10;
        List<String> contents = new ArrayList<>(testAmount);

        for (int i = 0; i < testAmount; i++) {
            contents.add("Content " + i);
        }

        contents.forEach(
                content -> macroManager.prependAtMacro(macro, content)
        );

        List<String> expectedContent = contents.reversed();

        assertNotNull(macroManager.getAtMacro(macro));
        assertEquals(
                expectedContent,
                macroManager.getAtMacro(macro)
        );
    }

    @Test
    void renderAtMacro() {
        String macro = "macro";

        int testAmount = 10;
        List<String> contents = new ArrayList<>(testAmount);

        for (int i = 0; i < testAmount; i++) {
            contents.add("Content " + i);
        }

        contents.forEach(
                content -> macroManager.appendAtMacro(macro, content)
        );

        String expectedRendered =
                String.join("", contents);

        Optional<String> actualRendered =
                macroManager.renderTarget(new Macro(macro));

        assertTrue(actualRendered.isPresent());
        assertEquals(
                expectedRendered,
                actualRendered.get()
        );
    }

    @Test
    void renderAtMacro_returnsEmptyIfNotFound() {
        Optional<String> result =
                macroManager.renderTarget(
                        new Macro("unknown")
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void replaceAt_addsFlexiblePatternTarget() {
        String pattern = "red dragon";

        macroManager.replaceAt(
                pattern,
                "replacement"
        );

        assertEquals(1, macroManager.getTargets().size());

        ReplacementTarget target =
                macroManager.getTargets()
                        .iterator()
                        .next();

        assertInstanceOf(
                FlexiblePattern.class,
                target
        );

        assertEquals(
                pattern,
                ((FlexiblePattern) target).getSource()
        );
    }

    @Test
    void renderTarget_supportsFlexiblePattern() {
        FlexiblePattern target =
                new FlexiblePattern("red dragon");

        macroManager.replaceAt(
                target,
                "first"
        );

        macroManager.replaceAt(
                target,
                "second"
        );

        Optional<String> rendered =
                macroManager.renderTarget(target);

        assertTrue(rendered.isPresent());
        assertEquals(
                "firstsecond",
                rendered.get()
        );
    }

    @Test
    void targetsCanContainMacrosAndFlexiblePatterns() {
        Macro macro = new Macro("outlet");
        FlexiblePattern pattern =
                new FlexiblePattern("red dragon");

        macroManager.appendAtMacro(
                macro,
                "macro content"
        );

        macroManager.replaceAt(
                pattern,
                "pattern content"
        );

        Set<ReplacementTarget> targets =
                macroManager.getTargets();

        assertEquals(2, targets.size());
        assertTrue(targets.contains(macro));
        assertTrue(targets.contains(pattern));
    }

    @Test
    void renderingIsInvalidatedWhenMacroChanges() {
        Macro macro = new Macro("macro");

        macroManager.appendAtMacro(
                macro,
                "first"
        );

        assertEquals(
                "first",
                macroManager.renderTarget(macro).orElseThrow()
        );

        assertThrows(
                IllegalStateException.class,
                () -> macroManager.appendAtMacro(
                        macro,
                        "second"
                )
        );
    }

    @Test
    void renderingIsInvalidatedWhenFlexiblePatternChanges() {
        FlexiblePattern pattern =
                new FlexiblePattern("dragon");

        macroManager.replaceAt(
                pattern,
                "first"
        );

        assertEquals(
                "first",
                macroManager.renderTarget(pattern).orElseThrow()
        );

        assertThrows(
                IllegalStateException.class,
                () -> macroManager.replaceAt(
                        pattern,
                        "second"
                )
        );
    }
}