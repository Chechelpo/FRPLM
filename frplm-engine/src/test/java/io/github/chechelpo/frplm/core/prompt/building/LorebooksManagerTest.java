package io.github.chechelpo.frplm.core.prompt.building;

import io.github.chechelpo.frplm.core.prompt.TextType;
import io.github.chechelpo.frplm.domain.lorebook.LorebookContext;
import io.github.chechelpo.frplm.domain.lorebook.LorebookContextTestFactory;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import io.github.chechelpo.frplm.domain.lorebook.keywords.KeywordServiceTestFactory;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletServiceTestFactory;
import io.github.chechelpo.frplm.extensions.api.prompts.LorebookManager;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static io.github.chechelpo.frplm.jooq.generated.Tables.OUTLET;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LorebooksManagerTest {

    private static final DSLContext JOOQ =
            DSL.using(SQLDialect.DEFAULT);

    @Mock
    private EntryService entryService;

    @Mock
    private LorebookService lorebookService;

    @Mock
    private EntryKeywordService entryKeywordService;

    private KeywordService keywordService;

    private OutletService outletService;

    @Mock
    private PromptBudgetManager budgetManager;

    @Mock
    private PromptRenderer renderer;

    private LorebooksManager manager;

    @BeforeEach
    void setUp() {
        keywordService = KeywordServiceTestFactory.mockService();
        outletService = OutletServiceTestFactory.mockService();

        LorebookContext context =
                LorebookContextTestFactory.create(
                        entryService,
                        lorebookService,
                        entryKeywordService,
                        keywordService,
                        outletService
                );

        manager = new LorebooksManager(
                context,
                budgetManager
        );
    }

    @Test
    void addsLorebooks() {
        LorebookSnapshot first = lorebook(1);
        LorebookSnapshot second = lorebook(2);
        LorebookSnapshot third = lorebook(3);

        manager.addLorebook(first);
        manager.addLorebooks(List.of(second, third));

        assertEquals(
                List.of(first, second, third),
                manager.getLorebooks()
        );

        assertEquals(
                List.of(first, second, third),
                manager.usedLorebooks()
        );
    }

    @Test
    void outletExistsDelegatesToOutletService() {
        when(outletService.getOutletID("known"))
                .thenReturn(Optional.of(9));

        when(outletService.getOutletID("unknown"))
                .thenReturn(Optional.empty());

        assertTrue(manager.outletExists("known"));
        assertFalse(manager.outletExists("unknown"));
    }

    @Test
    void rejectsOverrideForInactiveLorebook() {
        LorebookSnapshot inactive = lorebook(17);

        LorebookManager.OverrideResult result =
                manager.overrideLorebookOutlet(
                        inactive,
                        "old_outlet",
                        "new_outlet"
                );

        assertEquals(
                LorebookManager.OverrideResult
                        .TARGET_LOREBOOK_DOES_NOT_EXIST,
                result
        );

        verifyNoInteractions(outletService);
    }

    @Test
    void rejectsOverrideWhenTargetOutletDoesNotExist() {
        LorebookSnapshot lorebook = lorebook(17);
        manager.addLorebook(lorebook);

        when(outletService.getOutletID("missing"))
                .thenReturn(Optional.empty());

        LorebookManager.OverrideResult result =
                manager.overrideLorebookOutlet(
                        lorebook,
                        "missing",
                        "replacement"
                );

        assertEquals(
                LorebookManager.OverrideResult
                        .TARGET_OUTLET_DOES_NOT_EXIST,
                result
        );

        verify(outletService, never())
                .getOrCreateOutlet(anyString());
    }

    @Test
    void rejectsSecondLorebookWideOverride() {
        LorebookSnapshot lorebook = lorebook(4);
        manager.addLorebook(lorebook);

        when(outletService.getOrCreateOutlet("first"))
                .thenReturn(100);

        assertEquals(
                LorebookManager.OverrideResult.SUCCESS,
                manager.overrideAllLorebookOutlets(
                        lorebook,
                        "first"
                )
        );

        assertEquals(
                LorebookManager.OverrideResult.ALREADY_OVERRIDDEN,
                manager.overrideAllLorebookOutlets(
                        lorebook,
                        "second"
                )
        );

        verify(outletService, never())
                .getOrCreateOutlet("second");
    }

    @Test
    void appliesOutletOverridesInSpecificityOrder() {
        LorebookSnapshot firstLorebook = lorebook(1);
        LorebookSnapshot secondLorebook = lorebook(2);

        manager.addLorebooks(
                List.of(firstLorebook, secondLorebook)
        );

        /*
         * Global:
         * outlet 10 -> outlet 300
         */
        when(outletService.getOutletID("source-ten"))
                .thenReturn(Optional.of(10));

        when(outletService.getOrCreateOutlet("global"))
                .thenReturn(300);

        assertEquals(
                LorebookManager.OverrideResult.SUCCESS,
                manager.overrideOutlet(
                        "source-ten",
                        "global"
                )
        );

        /*
         * Lorebook-wide:
         * all entries in lorebook 1 -> outlet 200
         */
        when(outletService.getOrCreateOutlet("lorebook-wide"))
                .thenReturn(200);

        assertEquals(
                LorebookManager.OverrideResult.SUCCESS,
                manager.overrideAllLorebookOutlets(
                        firstLorebook,
                        "lorebook-wide"
                )
        );

        /*
         * Most specific:
         * lorebook 1, outlet 10 -> outlet 100
         */
        when(outletService.getOrCreateOutlet("specific"))
                .thenReturn(100);

        assertEquals(
                LorebookManager.OverrideResult.SUCCESS,
                manager.overrideLorebookOutlet(
                        firstLorebook,
                        "source-ten",
                        "specific"
                )
        );

        EntryRecord specific = constantEntry(
                1,
                1,
                10,
                0,
                "Specific override"
        );

        EntryRecord lorebookWide = constantEntry(
                1,
                2,
                11,
                0,
                "Lorebook-wide override"
        );

        EntryRecord global = constantEntry(
                2,
                1,
                10,
                0,
                "Global override"
        );

        EntryRecord unchanged = constantEntry(
                2,
                2,
                12,
                0,
                "Original outlet"
        );

        prepareConstantActivation(
                entries(
                        specific,
                        lorebookWide,
                        global,
                        unchanged
                )
        );

        manager.activateEntries(
                renderer,
                entryKeywordService
        );

        assertEquals(
                List.of(specific),
                manager.getOf(100).orElseThrow()
        );

        assertEquals(
                List.of(lorebookWide),
                manager.getOf(200).orElseThrow()
        );

        assertEquals(
                List.of(global),
                manager.getOf(300).orElseThrow()
        );

        assertEquals(
                List.of(unchanged),
                manager.getOf(12).orElseThrow()
        );
    }

    @Test
    void activatesAndSortsConstantEntriesWithinOutlet() {
        manager.addLorebooks(List.of(
                lorebook(1),
                lorebook(2)
        ));

        EntryRecord third = constantEntry(
                2,
                8,
                4,
                20,
                "Third"
        );

        EntryRecord second = constantEntry(
                1,
                9,
                4,
                20,
                "Second"
        );

        EntryRecord first = constantEntry(
                1,
                3,
                4,
                10,
                "First"
        );

        prepareConstantActivation(
                entries(third, second, first)
        );

        manager.activateEntries(
                renderer,
                entryKeywordService
        );

        List<EntryRecord> active =
                manager.getOf(4).orElseThrow();

        assertEquals(
                List.of(first, second, third),
                active
        );

        assertTrue(manager.isActive(1, 3));
        assertTrue(manager.isActive(1, 9));
        assertTrue(manager.isActive(2, 8));

        assertEquals(
                List.of(first, second, third),
                manager.activatedEntries()
        );
    }

    @Test
    void recursivelyActivatesEntryFromPreviouslyActivatedContent() {
        manager.addLorebook(lorebook(1));

        EntryRecord seed = constantEntry(
                1,
                1,
                4,
                0,
                "A dragon sleeps below the mountain."
        );

        EntryRecord recursive = entry(
                1,
                2,
                4,
                1,
                "The dragon-related recursive entry.",
                ActivationStrategy.COMMON
        );

        Result<EntryRecord> entries =
                entries(seed, recursive);

        when(entryService.getAllActiveEntriesOf(
                any(IntSet.class)
        )).thenReturn(entries);

        when(renderer.getChatHistory())
                .thenReturn(List.of());

        when(entryKeywordService.getKeywords(
                any(IntSet.class)
        )).thenReturn(List.of(
                IntObjectPair.of(50, "dragon")
        ));

        when(entryKeywordService.keywordIDsOfEntry(1, 1))
                .thenReturn(Set.of());

        when(entryKeywordService.keywordIDsOfEntry(1, 2))
                .thenReturn(Set.of(50));

        when(budgetManager.hasSpaceFor(
                anyString(),
                eq(TextType.LOREBOOK_ENTRY)
        )).thenReturn(true);

        manager.activateEntries(
                renderer,
                entryKeywordService
        );

        assertTrue(manager.isActive(1, 1));
        assertTrue(manager.isActive(1, 2));

        assertEquals(
                List.of(seed, recursive),
                manager.getOf(4).orElseThrow()
        );
    }

    @Test
    void preventFurtherRecursionStopsContentFromActivatingEntries() {
        manager.addLorebook(lorebook(1));

        EntryRecord seed = constantEntry(
                1,
                1,
                4,
                0,
                "A dragon sleeps below the mountain."
        );

        seed.setPreventFurtherRecursion(true);

        EntryRecord recursive = entry(
                1,
                2,
                4,
                1,
                "Recursive content",
                ActivationStrategy.COMMON
        );

        when(entryService.getAllActiveEntriesOf(
                any(IntSet.class)
        )).thenReturn(entries(seed, recursive));

        when(renderer.getChatHistory())
                .thenReturn(List.of());

        when(entryKeywordService.getKeywords(
                any(IntSet.class)
        )).thenReturn(List.of(
                IntObjectPair.of(50, "dragon")
        ));

        when(entryKeywordService.keywordIDsOfEntry(1, 1))
                .thenReturn(Set.of());

        when(entryKeywordService.keywordIDsOfEntry(1, 2))
                .thenReturn(Set.of(50));

        when(budgetManager.hasSpaceFor(
                anyString(),
                eq(TextType.LOREBOOK_ENTRY)
        )).thenReturn(true);

        manager.activateEntries(
                renderer,
                entryKeywordService
        );

        assertTrue(manager.isActive(1, 1));
        assertFalse(manager.isActive(1, 2));

        assertEquals(
                List.of(seed),
                manager.getOf(4).orElseThrow()
        );
    }

    @Test
    void getOutletsUsesActivatedOutletIds() {
        manager.addLorebook(lorebook(1));

        EntryRecord first = constantEntry(
                1,
                1,
                4,
                0,
                "First"
        );

        EntryRecord second = constantEntry(
                1,
                2,
                77,
                0,
                "Second"
        );

        prepareConstantActivation(
                entries(first, second)
        );

        manager.activateEntries(
                renderer,
                entryKeywordService
        );

        Result<Record2<Integer, String>> expected =
                JOOQ.newResult(
                        OUTLET.ID,
                        OUTLET.OUTLET_
                );

        when(outletService.getOutletsFromIds(
                any(IntSet.class)
        )).thenReturn(expected);

        assertSame(
                expected,
                manager.getOutlets()
        );

        ArgumentCaptor<IntSet> idsCaptor =
                ArgumentCaptor.forClass(IntSet.class);

        verify(outletService)
                .getOutletsFromIds(idsCaptor.capture());

        IntSet requestedIds = idsCaptor.getValue();

        assertEquals(2, requestedIds.size());
        assertTrue(requestedIds.contains(4));
        assertTrue(requestedIds.contains(77));
    }

    @Test
    void activateEntriesCannotBeCalledTwice() {
        manager.addLorebook(lorebook(1));

        prepareConstantActivation(entries());

        manager.activateEntries(
                renderer,
                entryKeywordService
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> manager.activateEntries(
                        renderer,
                        entryKeywordService
                )
        );

        assertEquals(
                "Activate entries has been called twice",
                exception.getMessage()
        );
    }

    @Test
    void budgetRejectedEntryIsActivatedButNotInjected() {
        manager.addLorebook(lorebook(1));

        EntryRecord entry = constantEntry(
                1,
                1,
                4,
                0,
                "Too large"
        );

        when(entryService.getAllActiveEntriesOf(
                any(IntSet.class)
        )).thenReturn(entries(entry));

        when(renderer.getChatHistory())
                .thenReturn(List.of());

        when(entryKeywordService.getKeywords(
                any(IntSet.class)
        )).thenReturn(List.of());

        when(entryKeywordService.keywordIDsOfEntry(1, 1))
                .thenReturn(Set.of());

        when(budgetManager.hasSpaceFor(
                "Too large",
                TextType.LOREBOOK_ENTRY
        )).thenReturn(false);

        manager.activateEntries(
                renderer,
                entryKeywordService
        );

        // The entry satisfied its activation strategy.
        assertTrue(manager.isActive(1, 1));

        // It was rejected by the prompt budget, so it is not injected.
        assertTrue(manager.activatedEntries().isEmpty());
        assertTrue(manager.getOf(4).isEmpty());
    }
    private void prepareConstantActivation(
            Result<EntryRecord> entries
    ) {
        when(entryService.getAllActiveEntriesOf(
                any(IntSet.class)
        )).thenReturn(entries);

        when(renderer.getChatHistory())
                .thenReturn(List.of());

        when(entryKeywordService.getKeywords(
                any(IntSet.class)
        )).thenReturn(List.of());

        lenient().when(entryKeywordService.keywordIDsOfEntry(
                anyInt(),
                anyInt()
        )).thenReturn(Set.of());

        lenient().when(budgetManager.hasSpaceFor(
                anyString(),
                eq(TextType.LOREBOOK_ENTRY)
        )).thenReturn(true);
    }

    private static LorebookSnapshot lorebook(int id) {
        LorebookSnapshot snapshot = mock(
                LorebookSnapshot.class,
                RETURNS_DEEP_STUBS
        );

        lenient().
                when(snapshot.asReference().id())
                .thenReturn(id);

        /*
         * Used only by logging branches. Lenient prevents strict Mockito
         * from rejecting it in successful paths.
         */
        lenient()
                .when(snapshot.getName())
                .thenReturn("Lorebook " + id);

        return snapshot;
    }

    private static EntryRecord constantEntry(
            int lorebookId,
            int entryId,
            int outletId,
            int position,
            String content
    ) {
        return entry(
                lorebookId,
                entryId,
                outletId,
                position,
                content,
                ActivationStrategy.CONSTANT
        );
    }

    private static EntryRecord entry(
            int lorebookId,
            int entryId,
            int outletId,
            int position,
            String content,
            ActivationStrategy strategy
    ) {
        EntryRecord entry =
                JOOQ.newRecord(ENTRY);

        entry.setLorebookId(lorebookId);
        entry.setEntryId(entryId);
        entry.setName("Entry " + entryId);
        entry.setContent(content);
        entry.setOutlet(outletId);
        entry.setPosition((short) position);
        entry.setStrategy(strategy.stable_id);
        entry.setProbability((short) 100);
        entry.setNonRecursable(false);
        entry.setPreventFurtherRecursion(false);
        entry.setScanDepth(null);

        return entry;
    }

    private static Result<EntryRecord> entries(
            EntryRecord... entries
    ) {
        Result<EntryRecord> result =
                JOOQ.newResult(ENTRY);

        Collections.addAll(result, entries);

        return result;
    }
}