package chechelpo.frplm.core.prompt.building;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LorebooksManagerTest {

    @Test
    void constructorRejectsNullContext() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new LorebooksManager(null, null)
        );

        assertEquals("Lorebook context is null", exception.getMessage());
    }
/*
    @Test
    void newlyConstructedManagerHasNoLorebooksAndNoActiveEntries() {
        LorebooksManager manager = new LorebooksManager(contextReturningEntries(List.of()).context());

        assertTrue(manager.getLorebooks().isEmpty());
        assertTrue(manager.getOf(1).isEmpty());
        assertFalse(manager.isActive(1, 1));
    }

    @Test
    void addLorebookAppendsSnapshot() {
        LorebooksManager manager = new LorebooksManager(contextReturningEntries(List.of()).context());

        LorebookSnapshot first = lorebook(10);
        LorebookSnapshot second = lorebook(20);

        manager.addLorebook(first);
        manager.addLorebook(second);

        assertEquals(List.of(first, second), manager.getLorebooks());
    }

    @Test
    void addLorebooksAppendsSnapshotsInOrder() {
        LorebooksManager manager = new LorebooksManager(contextReturningEntries(List.of()).context());

        LorebookSnapshot first = lorebook(10);
        LorebookSnapshot second = lorebook(20);
        LorebookSnapshot third = lorebook(30);

        manager.addLorebook(first);
        manager.addLorebooks(List.of(second, third));

        assertEquals(List.of(first, second, third), manager.getLorebooks());
    }

    @Test
    void getLorebooksReturnsLiveBackingList() {
        LorebooksManager manager = new LorebooksManager(contextReturningEntries(List.of()).context());

        LorebookSnapshot snapshot = lorebook(10);
        List<LorebookSnapshot> returned = manager.getLorebooks();

        returned.add(snapshot);

        assertEquals(List.of(snapshot), manager.getLorebooks());
    }

    @Test
    void activateEntriesPassesConfiguredLorebookIdsToContextRepository() {
        EntryRecord entry = entry(
                10,
                100,
                7,
                "Entry content",
                false,
                false
        );

        ContextFixture fixture = contextReturningEntries(List.of(entry));

        LorebooksManager manager = new LorebooksManager(fixture.context());
        manager.addLorebooks(List.of(lorebook(10), lorebook(20)));

        EntryKeywordService keywordService = keywordService(
                List.of(keyword(1, "dragon"))
        );

        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(same(entry), eq(0), eq(0)))
                    .thenReturn(true);

            manager.activateEntries(rendererWithMessages(message("dragon")));
        }

        IntSet passedLorebookIds = fixture.lastRequestedLorebookIds().get();

        assertNotNull(passedLorebookIds);
        assertEquals(new IntOpenHashSet(new int[]{10, 20}), passedLorebookIds);
    }

    @Test
    void activateEntriesMarksFirstPassEntryActiveWhenAllKeywordsAreDetected() {
        EntryRecord entry = entry(
                10,
                100,
                7,
                "Lorebook content",
                false,
                false
        );

        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of(entry)).context()
        );
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(keyword(1, "dragon"))
        );
        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(same(entry), eq(0), eq(0)))
                    .thenReturn(true);

            manager.activateEntries(
                    rendererWithMessages(message("A dragon appears.")),
                    keywordService
            );
        }

        assertTrue(manager.isActive(10, 100));

        Optional<List<EntryRecord>> outletEntries = manager.getOf(7);
        assertTrue(outletEntries.isPresent());
        assertEquals(List.of(entry), outletEntries.get());
    }

    @Test
    void activateEntriesDoesNotMarkEntryActiveWhenStaticEvaluatorReturnsFalse() {
        EntryRecord entry = entry(
                10,
                100,
                7,
                "Lorebook content",
                false,
                false
        );

        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of(entry)).context()
        );
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(keyword(1, "dragon"))
        );
        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(same(entry), eq(0), eq(0)))
                    .thenReturn(false);

            manager.activateEntries(
                    rendererWithMessages(message("A dragon appears.")),
                    keywordService
            );
        }

        assertFalse(manager.isActive(10, 100));
        assertTrue(manager.getOf(7).isEmpty());
    }

    @Test
    void activateEntriesGroupsActiveEntriesByOutlet() {
        EntryRecord first = entry(
                10,
                100,
                7,
                "First content",
                false,
                false
        );

        EntryRecord second = entry(
                10,
                101,
                7,
                "Second content",
                false,
                false
        );

        EntryRecord third = entry(
                10,
                102,
                9,
                "Third content",
                false,
                false
        );

        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of(first, second, third)).context()
        );
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(
                        keyword(1, "dragon"),
                        keyword(2, "castle"),
                        keyword(3, "wizard")
                )
        );

        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1));
        when(keywordService.keywordIDsOfEntry(10, 101)).thenReturn(Set.of(2));
        when(keywordService.keywordIDsOfEntry(10, 102)).thenReturn(Set.of(3));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(any(EntryRecord.class), eq(0), eq(0)))
                    .thenReturn(true);

            manager.activateEntries(
                    rendererWithMessages(message("dragon castle wizard")),
                    keywordService
            );
        }

        assertEquals(List.of(first, second), manager.getOf(7).orElseThrow());
        assertEquals(List.of(third), manager.getOf(9).orElseThrow());

        assertTrue(manager.isActive(10, 100));
        assertTrue(manager.isActive(10, 101));
        assertTrue(manager.isActive(10, 102));
    }

    @Test
    void activateEntriesDoesNotDuplicateSameEntryWhenRepositoryReturnsDuplicateRecords() {
        EntryRecord entry = entry(
                10,
                100,
                7,
                "Duplicate content",
                false,
                false
        );

        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of(entry, entry)).context()
        );
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(keyword(1, "dragon"))
        );
        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(same(entry), eq(0), eq(0)))
                    .thenReturn(true);

            manager.activateEntries(
                    rendererWithMessages(message("dragon")),
                    keywordService
            );
        }

        assertEquals(List.of(entry), manager.getOf(7).orElseThrow());
        assertTrue(manager.isActive(10, 100));
    }

    @Test
    void activateEntriesUsesDeepestDetectedKeywordDepthForFirstPassActivation() {
        EntryRecord entry = entry(
                10,
                100,
                7,
                "Lorebook content",
                false,
                false
        );

        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of(entry)).context()
        );
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(
                        keyword(1, "dragon"),
                        keyword(2, "castle")
                )
        );

        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1, 2));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(same(entry), eq(0), eq(2)))
                    .thenReturn(true);

            manager.activateEntries(
                    rendererWithMessages(
                            message("dragon"),
                            message("irrelevant"),
                            message("castle")
                    ),
                    keywordService
            );

            evaluator.verify(() -> EntryEvaluator.activates(same(entry), eq(0), eq(2)));
        }

        assertTrue(manager.isActive(10, 100));
    }

    @Test
    void activateEntriesQueuesRecursableEntryAndActivatesItAfterFirstPassContentAddsKeyword() {
        EntryRecord seed = entry(
                10,
                100,
                7,
                "The ancient key is hidden here.",
                false,
                false
        );

        EntryRecord recursive = entry(
                10,
                101,
                8,
                "Recursive entry content",
                false,
                false
        );

        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of(seed, recursive)).context()
        );
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(
                        keyword(1, "dragon"),
                        keyword(2, "ancient key")
                )
        );

        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1));
        when(keywordService.keywordIDsOfEntry(10, 101)).thenReturn(Set.of(2));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(same(seed), eq(0), eq(0)))
                    .thenReturn(true);

            evaluator.when(() -> EntryEvaluator.activates(same(recursive), eq(0), eq(0)))
                    .thenReturn(true);

            manager.activateEntries(
                    rendererWithMessages(message("dragon")),
                    keywordService
            );
        }

        assertTrue(manager.isActive(10, 100));
        assertTrue(manager.isActive(10, 101));

        assertEquals(List.of(seed), manager.getOf(7).orElseThrow());
        assertEquals(List.of(recursive), manager.getOf(8).orElseThrow());
    }

    @Test
    void activateEntriesDoesNotUseFirstPassEntryContentForRecursionWhenPreventFurtherRecursionIsTrue() {
        EntryRecord seed = entry(
                10,
                100,
                7,
                "The ancient key is hidden here.",
                true,
                false
        );

        EntryRecord recursive = entry(
                10,
                101,
                8,
                "Recursive entry content",
                false,
                false
        );

        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of(seed, recursive)).context()
        );
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(
                        keyword(1, "dragon"),
                        keyword(2, "ancient key")
                )
        );

        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1));
        when(keywordService.keywordIDsOfEntry(10, 101)).thenReturn(Set.of(2));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(same(seed), eq(0), eq(0)))
                    .thenReturn(true);

            manager.activateEntries(
                    rendererWithMessages(message("dragon")),
                    keywordService
            );

            evaluator.verify(
                    () -> EntryEvaluator.activates(same(recursive), anyInt(), anyInt()),
                    never()
            );
        }

        assertTrue(manager.isActive(10, 100));
        assertFalse(manager.isActive(10, 101));

        assertEquals(List.of(seed), manager.getOf(7).orElseThrow());
        assertTrue(manager.getOf(8).isEmpty());
    }

    @Test
    void activateEntriesDoesNotQueueEntryForRecursionWhenEntryIsNonRecursable() {
        EntryRecord seed = entry(
                10,
                100,
                7,
                "The ancient key is hidden here.",
                false,
                false
        );

        EntryRecord nonRecursable = entry(
                10,
                101,
                8,
                "Non-recursable content",
                false,
                true
        );

        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of(seed, nonRecursable)).context()
        );
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(
                        keyword(1, "dragon"),
                        keyword(2, "ancient key")
                )
        );

        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1));
        when(keywordService.keywordIDsOfEntry(10, 101)).thenReturn(Set.of(2));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(same(seed), eq(0), eq(0)))
                    .thenReturn(true);

            manager.activateEntries(
                    rendererWithMessages(message("dragon")),
                    keywordService
            );

            evaluator.verify(
                    () -> EntryEvaluator.activates(same(nonRecursable), anyInt(), anyInt()),
                    never()
            );
        }

        assertTrue(manager.isActive(10, 100));
        assertFalse(manager.isActive(10, 101));
    }

    @Test
    void activateEntriesCanActivateMultipleRecursiveWaves() {
        EntryRecord first = entry(
                10,
                100,
                1,
                "This reveals beta.",
                false,
                false
        );

        EntryRecord second = entry(
                10,
                101,
                2,
                "This reveals gamma.",
                false,
                false
        );

        EntryRecord third = entry(
                10,
                102,
                3,
                "Final recursive content.",
                false,
                false
        );

        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of(first, second, third)).context()
        );
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(
                        keyword(1, "alpha"),
                        keyword(2, "beta"),
                        keyword(3, "gamma")
                )
        );

        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1));
        when(keywordService.keywordIDsOfEntry(10, 101)).thenReturn(Set.of(2));
        when(keywordService.keywordIDsOfEntry(10, 102)).thenReturn(Set.of(3));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(any(EntryRecord.class), eq(0), eq(0)))
                    .thenReturn(true);

            manager.activateEntries(
                    rendererWithMessages(message("alpha")),
                    keywordService
            );
        }

        assertTrue(manager.isActive(10, 100));
        assertTrue(manager.isActive(10, 101));
        assertTrue(manager.isActive(10, 102));

        assertEquals(List.of(first), manager.getOf(1).orElseThrow());
        assertEquals(List.of(second), manager.getOf(2).orElseThrow());
        assertEquals(List.of(third), manager.getOf(3).orElseThrow());
    }

    @Test
    void activateEntriesStopsRecursionWhenNoNewKeywordsWereDetected() {
        EntryRecord first = entry(
                10,
                100,
                1,
                "This content does not reveal the missing keyword.",
                false,
                false
        );

        EntryRecord blocked = entry(
                10,
                101,
                2,
                "Blocked content.",
                false,
                false
        );

        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of(first, blocked)).context()
        );
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(
                        keyword(1, "alpha"),
                        keyword(2, "beta")
                )
        );

        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1));
        when(keywordService.keywordIDsOfEntry(10, 101)).thenReturn(Set.of(2));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(same(first), eq(0), eq(0)))
                    .thenReturn(true);

            manager.activateEntries(
                    rendererWithMessages(message("alpha")),
                    keywordService
            );

            evaluator.verify(
                    () -> EntryEvaluator.activates(same(blocked), anyInt(), anyInt()),
                    never()
            );
        }

        assertTrue(manager.isActive(10, 100));
        assertFalse(manager.isActive(10, 101));
    }

    @Test
    void activateEntriesWithNoLorebooksDoesNotActivateAnything() {
        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of()).context()
        );

        EntryKeywordService keywordService = keywordService(List.of());

        manager.activateEntries(
                rendererWithMessages(message("dragon")),
                keywordService
        );

        assertTrue(manager.getLorebooks().isEmpty());
        assertTrue(manager.getOf(1).isEmpty());
        assertFalse(manager.isActive(1, 1));
    }

    @Test
    void activateEntriesWithNoRepositoryEntriesDoesNotActivateAnything() {
        LorebooksManager manager = new LorebooksManager(
                contextReturningEntries(List.of()).context()
        );
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(keyword(1, "dragon"))
        );

        manager.activateEntries(
                rendererWithMessages(message("dragon")),
                keywordService
        );

        assertTrue(manager.getOf(1).isEmpty());
        assertFalse(manager.isActive(10, 100));
    }

    @Test
    void activateEntriesAccumulatesActiveEntriesAcrossMultipleCallsBecauseStateIsNotCleared() {
        EntryRecord first = entry(
                10,
                100,
                1,
                "First content",
                false,
                false
        );

        EntryRecord second = entry(
                10,
                101,
                1,
                "Second content",
                false,
                false
        );

        ContextFixture fixture = contextReturningEntries(List.of(first));
        LorebooksManager manager = new LorebooksManager(fixture.context());
        manager.addLorebook(lorebook(10));

        EntryKeywordService keywordService = keywordService(
                List.of(
                        keyword(1, "dragon"),
                        keyword(2, "castle")
                )
        );

        when(keywordService.keywordIDsOfEntry(10, 100)).thenReturn(Set.of(1));
        when(keywordService.keywordIDsOfEntry(10, 101)).thenReturn(Set.of(2));

        try (MockedStatic<EntryEvaluator> evaluator = mockStatic(EntryEvaluator.class)) {
            evaluator.when(() -> EntryEvaluator.activates(any(EntryRecord.class), eq(0), eq(0)))
                    .thenReturn(true);

            manager.activateEntries(
                    rendererWithMessages(message("dragon")),
                    keywordService
            );

            fixture.replaceEntries(List.of(second));

            manager.activateEntries(
                    rendererWithMessages(message("castle")),
                    keywordService
            );
        }

        assertEquals(List.of(first, second), manager.getOf(1).orElseThrow());
        assertTrue(manager.isActive(10, 100));
        assertTrue(manager.isActive(10, 101));
    }

    private static EntryRecord entry(
            int lorebookId,
            int entryId,
            int outlet,
            String content,
            boolean preventFurtherRecursion,
            boolean nonRecursable
    ) {
        EntryRecord record = new EntryRecord();

        record.setLorebookId(lorebookId);
        record.setEntryId(entryId);
        record.setOutlet(outlet);
        record.setContent(content);
        record.setPreventFurtherRecursion(preventFurtherRecursion);
        record.setNonRecursable(nonRecursable);

        return record;
    }

    private static EntryKeywordService keywordService(
            List<IntObjectPair<String>> allKeywords
    ) {
        EntryKeywordService keywordService = mock(EntryKeywordService.class);
        when(keywordService.getKeywords(any(IntSet.class))).thenReturn(allKeywords);
        return keywordService;
    }

    private static IntObjectPair<String> keyword(int id, String value) {
        return IntObjectPair.of(id, value);
    }

    private static PromptRenderer rendererWithMessages(ChatMessage... messages) {
        PromptRenderer renderer = mock(PromptRenderer.class);
        when(renderer.getChatHistory()).thenReturn(List.of(messages));
        return renderer;
    }

    private static ChatMessage message(String content) {
        ChatMessage message = mock(ChatMessage.class);
        when(message.content()).thenReturn(content);
        return message;
    }

    private static LorebookSnapshot lorebook(int id) {
        LorebookSnapshot snapshot = mock(LorebookSnapshot.class, RETURNS_DEEP_STUBS);
        when(snapshot.reference().id()).thenReturn(id);
        return snapshot;
    }

    private static ContextFixture contextReturningEntries(List<EntryRecord> entries) {
        try {
            AtomicReference<List<EntryRecord>> mutableEntries =
                    new AtomicReference<>(new ArrayList<>(entries));

            AtomicReference<IntSet> lastRequestedLorebookIds =
                    new AtomicReference<>();

            LorebookContext context = mock(LorebookContext.class);

            Field entriesField = LorebookContext.class.getDeclaredField("entries");
            entriesField.setAccessible(true);

            Object entriesRepository = mock(
                    entriesField.getType(),
                    invocation -> {
                        if (invocation.getMethod().getName().equals("getAllActiveEntriesOf")) {
                            lastRequestedLorebookIds.set(invocation.getArgument(0));
                            return resultOf(mutableEntries.get());
                        }

                        return RETURNS_DEFAULTS.answer(invocation);
                    }
            );

            entriesField.set(context, entriesRepository);

            return new ContextFixture(
                    context,
                    mutableEntries,
                    lastRequestedLorebookIds
            );
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not build LorebookContext test fixture", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Result<EntryRecord> resultOf(List<EntryRecord> entries) {
        Result<EntryRecord> result = mock(Result.class);

        when(result.size()).thenReturn(entries.size());
        when(result.iterator()).thenAnswer(invocation -> entries.iterator());

        return result;
    }

    private record ContextFixture(
            LorebookContext context,
            AtomicReference<List<EntryRecord>> entries,
            AtomicReference<IntSet> lastRequestedLorebookIds
    ) {
        void replaceEntries(List<EntryRecord> replacement) {
            entries.set(new ArrayList<>(replacement));
        }
    }*/
}