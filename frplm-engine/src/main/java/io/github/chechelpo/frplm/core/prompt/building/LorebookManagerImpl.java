package io.github.chechelpo.frplm.core.prompt.building;

import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.core.prompt.TextType;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.extensions.api.standalone.EntrySnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.utils.collections.IntSetFactory;
import io.github.chechelpo.frplm.extensions.api.prompts.LorebookManager;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.*;
import org.jooq.Result;
import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class LorebookManagerImpl implements LorebookManager {
    private static final Logger log = (Logger) LoggerFactory.getLogger("Lorebook Manager");

    private final EntryService entryService;
    private final EntryKeywordService entryKeywordService;
    private final PromptBudgetManager budgetManager;
    private final List<LorebookSnapshot> lorebooks = new ArrayList<>(10);

    private boolean hasCheckedEntries;
    private final List<EntryRecord> activeEntryList = new ArrayList<>();
    private final Set<IntIntPair> activeEntriesIds = new HashSet<>();


    LorebookManagerImpl(
            EntryService entryService,
            EntryKeywordService entryKeywordService,
            PromptBudgetManager budgetManager
    ){
        Objects.requireNonNull(entryService);
        Objects.requireNonNull(entryKeywordService, "Lorebook context is null");

        this.entryKeywordService = entryKeywordService;
        this.entryService = entryService;
        this.budgetManager = budgetManager;
    }

    @Override
    public void addLorebook(LorebookSnapshot snapshot){
        Objects.requireNonNull(snapshot);
        lorebooks.add(snapshot);
    }
    @Override
    public void addLorebooks(List<LorebookSnapshot> snapshots){
        Objects.requireNonNull(snapshots);
        lorebooks.addAll(snapshots);
    }
    @Override
    public List<LorebookSnapshot> activeLorebooks(){
        return lorebooks;
    }
    @Override
    public boolean containsLorebook(LorebookSnapshot lorebook){
        return lorebook != null && lorebooks.stream().anyMatch(other -> other.sameEntityAs(lorebook));
    }
    @Override
    public boolean entryIsActive(EntrySnapshot.Reference entryReference) {
        return entryReference != null && isActive(entryReference.lorebookId(), entryReference.entryId());
    }

    public boolean isActive(int lorebookId, int entryId){
        return activeEntriesIds.contains(IntIntPair.of(lorebookId, entryId));
    }
    public List<EntryRecord> activeEntries(){
        if (!hasCheckedEntries)
            throw new IllegalStateException("Calling for active entries before activating them");
        return activeEntryList;
    }

    /**
     * TODO : Optimize this shit. Thank GOD I'm using H2 instead of going through JNI on each call
     */
    void activateEntries(PromptRenderer builder){
        if (hasCheckedEntries)
            throw new IllegalStateException("Activate entries has been called twice");
        hasCheckedEntries = true;

        IntSet lorebookIds = IntSetFactory.ofValues(
                lorebooks.stream()
                        .mapToInt(rec -> rec.asReference().id())
                        .toArray()
        );
        KeywordDetector keywordDetector = new KeywordDetector(builder, lorebookIds, entryKeywordService);

        int firstDetected = keywordDetector.getNumberOfDetectedKeywords();
        Result<EntryRecord> entries = entryService.getAllActiveEntriesOf(lorebookIds);
        Queue<Pair<EntryRecord, Set<Integer>>> recursableEntries = new ArrayDeque<>(entries.size());

        //First pass, evaluate all entries.
        List<EntryRecord> firstPassEntries = doFirstPass(entryKeywordService, entries, keywordDetector, recursableEntries);
        firstPassEntries.forEach(entry -> {
            if (!entry.getPreventFurtherRecursion()) keywordDetector.addContainedIn(entry);
        });

        recursiveActivateEntries(recursableEntries, firstDetected, keywordDetector);

        // Post process
        activeEntryList.sort(
                        Comparator
                                .comparingInt(EntryRecord::getLorebookId)
                                .thenComparingInt(EntryRecord::getPosition)
                                .thenComparingInt(EntryRecord::getEntryId)
                        );
    }

    private @NonNull List<EntryRecord> doFirstPass(
            EntryKeywordService entryKeywords,
            @NonNull Result<EntryRecord> entries,
            KeywordDetector keywordDetector,
            Queue<Pair<EntryRecord, Set<Integer>>> recursableEntries
    ) {
        List<EntryRecord> firstPassEntries = new ArrayList<>();
        for (EntryRecord entry : entries){
            // TODO: Here's the per entry DB query
            Set<Integer> keywordIds = entryKeywords.keywordIDsOfEntry(entry.getLorebookId(), entry.getEntryId());
            EntryEvaluator.EntryActivation activationResult = EntryEvaluator.entryActivates(
                    entry,
                    keywordIds,
                    0,
                    keywordDetector
            );

            switch (activationResult){
                case SUCCESS -> {
                    markActive(entry);
                    firstPassEntries.add(entry);
                }
                case KEYWORDS_MISSING -> {
                    if (!entry.getNonRecursable())
                        recursableEntries.add(Pair.of(entry, keywordIds));
                }
                case FAILED -> {}
            }
        }

        return firstPassEntries;
    }

    private void recursiveActivateEntries(
            Queue<Pair<EntryRecord, Set<Integer>>> recursableEntries,
            int previouslyDetected,
            KeywordDetector keywordDetector
    ) {
        int currentlyDetected = keywordDetector.getNumberOfDetectedKeywords();

        while (!recursableEntries.isEmpty() && currentlyDetected != previouslyDetected) {
            previouslyDetected = currentlyDetected;

            int entriesToCheckThisWave = recursableEntries.size();

            while (entriesToCheckThisWave-- > 0) {
                Pair<EntryRecord, Set<Integer>> entryAndKeywords = recursableEntries.poll();
                assert entryAndKeywords != null;
                EntryRecord entry = entryAndKeywords.first();
                assert entry != null;
                EntryEvaluator.EntryActivation activationResult = EntryEvaluator.entryActivates(
                        entry,
                        entryAndKeywords.second(),
                        0,
                        keywordDetector
                );

                switch (activationResult) {
                    case SUCCESS -> {
                        markActive(entryAndKeywords.first());
                        if (!entry.getPreventFurtherRecursion())
                            keywordDetector.addContainedIn(entry);
                    }
                    case KEYWORDS_MISSING -> recursableEntries.add(entryAndKeywords);
                    case FAILED -> {}
                }
            }

            currentlyDetected = keywordDetector.getNumberOfDetectedKeywords();
        }
    }

    private void markActive(@NonNull EntryRecord entry) {
        IntIntPair entryKey = IntIntPair.of(entry.getLorebookId(), entry.getEntryId());
        if (!activeEntriesIds.add(entryKey))  //Entry was already active
            return;
        if (!budgetManager.hasSpaceFor(entry.getContent(), TextType.LOREBOOK_ENTRY)) {
            log.debug("Couldn't add entry {}, ran out of budget space", entry.getName());
            return;
        }

        activeEntryList.add(entry);
    }

}