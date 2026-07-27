package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.utils.macros.Macro;
import io.github.chechelpo.frplm.utils.macros.Outlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MacroManagerTest {
    OutletManagerImpl outletManager;
    MacroManager macroManager;

    @BeforeEach
    void setUp(){
        outletManager = mock(OutletManagerImpl.class);
        macroManager = new MacroManager(outletManager);
    }

    @Test
    void testEntryAddition(){
        String outletName = "default";

        int testAmount = 10;
        List<EntryRecord> mockEntries = new ArrayList<>(testAmount);
        for (int i = 0 ; i < testAmount ; i++) {
            EntryRecord entryRecord = new EntryRecord();
            entryRecord.setContent("Content"+i);
            mockEntries.add(entryRecord);
        }
        when(outletManager.getOutletOf(any(EntryRecord.class)))
                .thenReturn(new Outlet(outletName));

        macroManager.addEntries(mockEntries);

        assertEquals(
                mockEntries.stream().map(EntryRecord::getContent).toList(),
                macroManager.getAtMacro(new Outlet(outletName))
        );
    }

    @Test
    void normalizesMacros(){
        int amount = 10;
        Set<String> expectedMacroNames = new HashSet<>();
        for (int i = 0 ; i < amount ; i++) expectedMacroNames.add("macro " + i);
        expectedMacroNames.forEach(macro -> macroManager.appendAtMacro(macro, "ignored"));

        Set<Macro> expectedMacros = expectedMacroNames.stream().map(Macro::new).collect(Collectors.toSet());
        Set<Macro> actualMacros = macroManager.getMacros();

        assertEquals(expectedMacros, actualMacros);
    }

    @Test
    void testAtIndex(){
        String macro = "macro";

        int testAmount = 10;
        List<String> expectedContents = new ArrayList<>(testAmount);
        for (int i = 0 ; i < testAmount ; i++) expectedContents.add("Content " + i);
        expectedContents.forEach(content -> macroManager.appendAtMacro(macro, content));

        String inserted = "inserted";
        macroManager.injectAtMacro(macro, inserted, 2);
        macroManager.injectAtMacro(macro, inserted, 4);

        List<String> actual = macroManager.getAtMacro(macro);
        assertEquals(inserted, actual.get(2));
        assertEquals(inserted, actual.get(4));
    }

    @Test
    void injectAtIndex_supportsHighIndexes(){
        String macro = "macro";

        int testAmount = 10;
        List<String> expectedContents = new ArrayList<>(testAmount);
        for (int i = 0 ; i < testAmount ; i++) expectedContents.add("Content " + i);
        expectedContents.forEach(content -> macroManager.appendAtMacro(macro, content));

        String inserted = "inserted";
        macroManager.injectAtMacro(macro, inserted, testAmount + 3);

        assertEquals(inserted, macroManager.getAtMacro(macro).get(testAmount));
    }

    @Test
    void injectAtIndex_supportsNegativeIndexes(){
        String macro = "macro";

        int testAmount = 10;
        List<String> expectedContents = new ArrayList<>(testAmount);
        for (int i = 0 ; i < testAmount ; i++) expectedContents.add("Content " + i);
        expectedContents.forEach(content -> macroManager.appendAtMacro(macro, content));

        String inserted = "inserted";
        macroManager.injectAtMacro(macro, inserted, -2);

        assertEquals(inserted, macroManager.getAtMacro(macro).get(testAmount - 2));
    }

    @Test
    void testAppend(){
        String macro = "macro";

        int testAmount = 10;
        List<String> expectedContents = new ArrayList<>(testAmount);
        for (int i = 0 ; i < testAmount ; i++) expectedContents.add("Content " + i);

        expectedContents.forEach(content -> macroManager.appendAtMacro(macro, content));

        assertNotNull(macroManager.getAtMacro(macro));
        assertEquals(expectedContents, macroManager.getAtMacro(macro));
    }

    @Test
    void testPrepend(){
        String macro = "macro";

        int testAmount = 10;
        List<String> contents = new ArrayList<>(testAmount);
        for (int i = 0 ; i < testAmount ; i++) contents.add("Content " + i);
        contents.forEach(content -> macroManager.prependAtMacro(macro, content));

        List<String> expectedContent = contents.reversed();

        assertNotNull(macroManager.getAtMacro(macro));
        assertEquals(expectedContent, macroManager.getAtMacro(macro));
    }

    @Test
    void renderAtMacro(){
        String macro = "macro";

        int testAmount = 10;
        List<String> contents = new ArrayList<>(testAmount);
        for (int i = 0 ; i < testAmount ; i++) contents.add("Content " + i);
        contents.forEach(content -> macroManager.appendAtMacro(macro, content));

        String expectedRendered = "";
        for (String content: contents) expectedRendered += content;

        Optional<String> actualRendered = macroManager.renderMacro(macro);
        assertTrue(actualRendered.isPresent());

        assertEquals(expectedRendered, actualRendered.get());
    }

    @Test
    void renderAtMacro_returnsEmptyIfNotFound() {
        assertFalse(macroManager.renderMacro("unknown").isPresent());
    }
}