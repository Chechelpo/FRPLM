package io.github.chechelpo.frplm.core.prompt.building;

import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.core.prompt.TextType;
import io.github.chechelpo.frplm.domain.lorebook.LorebookContext;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.utils.collections.IntSetFactory;
import io.github.chechelpo.frplm.extensions.api.prompts.LorebookManager;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.jooq.Record2;
import org.jooq.Result;
import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class LorebooksManager implements LorebookManager {
    private static final Logger log = (Logger) LoggerFactory.getLogger("Lorebook Manager");

    private final LorebookContext lorebookContext;
    private final PromptBudgetManager budgetManager;
    private final List<LorebookSnapshot> lorebooks = new ArrayList<>(10);

    private boolean hasCheckedEntries;
    private final Int2ObjectMap<List<EntryRecord>> activeEntriesByOutlet = new Int2ObjectArrayMap<>(20);
    private final Set<IntIntPair> activeEntriesIds = new HashSet<>();

    /** (lorebookId, outletId) -> override outlet */
    private final Object2IntMap<IntIntPair> lorebookOutletOverrides = new Object2IntArrayMap<>();
    /** (lorebookId) -> override outletId*/
    private final Int2IntArrayMap lorebookOverrides = new Int2IntArrayMap();
    /** outletId -> overrideOutlet */
    private final Int2IntArrayMap globalOutletOverrides = new Int2IntArrayMap();

    LorebooksManager(LorebookContext context, PromptBudgetManager budgetManager){
        Objects.requireNonNull(context, "Lorebook context is null");
        this.lorebookContext = context;
        this.budgetManager = budgetManager;
    }

    public void addLorebook(LorebookSnapshot snapshot){
        lorebooks.add(snapshot);
    }
    public void addLorebooks(List<LorebookSnapshot> snapshots){
        lorebooks.addAll(snapshots);
    }

    public boolean outletExists(String outletName){
        return lorebookContext.outlets.getOutletID(outletName).isPresent();
    }

    @Override
    public OverrideResult overrideLorebookOutlet(@NonNull LorebookSnapshot targetLorebook, String targetOutlet, String newOutlet) {
        Optional<LorebookSnapshot> found = lorebooks.stream().filter(other -> other.equals(targetLorebook))
                .findFirst();
        if (found.isEmpty()) {
            log.info("Lorebook {} is not active when overriding target outlet {} with {}", targetLorebook.getName(), targetOutlet, newOutlet);
            return OverrideResult.TARGET_LOREBOOK_DOES_NOT_EXIST;
        };

        int targetLorebookId = targetLorebook.asReference().id();
        Optional<Integer> previousOutletId = lorebookContext.outlets.getOutletID(targetOutlet);
        if (previousOutletId.isEmpty()){
            return OverrideResult.TARGET_OUTLET_DOES_NOT_EXIST;
        }
        int overrideOutletId = lorebookContext.outlets.getOrCreateOutlet(newOutlet);

        lorebookOutletOverrides.put(IntIntPair.of(targetLorebookId, previousOutletId.get()), overrideOutletId);
        return OverrideResult.SUCCESS;
    }

    @Override
    public OverrideResult overrideAllLorebookOutlets(LorebookSnapshot targetLorebook, String newOutletName) {
        Optional<LorebookSnapshot> found = lorebooks.stream()
                .filter(other -> other.equals(targetLorebook))
                .findFirst();
        if (found.isEmpty()) {
            log.info("Target lorebook {} is not active when overriding outlet {}", targetLorebook.getName(), newOutletName);
            return OverrideResult.TARGET_LOREBOOK_DOES_NOT_EXIST;
        }
        int lorebookId = targetLorebook.asReference().id();
        if (lorebookOverrides.containsKey(lorebookId)){
            log.debug("Lorebook {} already has an active lorebook-wide override: {}", targetLorebook.getName(), newOutletName);
            return OverrideResult.ALREADY_OVERRIDDEN;
        }
        lorebookOverrides.put(lorebookId, lorebookContext.outlets.getOrCreateOutlet(newOutletName));
        return OverrideResult.SUCCESS;
    }

    @Override
    public OverrideResult overrideOutlet(String targetOutlet, String newOutlet) {
        Optional<Integer> previousOutlet = lorebookContext.outlets.getOutletID(targetOutlet);
        if (previousOutlet.isEmpty()) return OverrideResult.TARGET_OUTLET_DOES_NOT_EXIST;

        int overrideOutlet = lorebookContext.outlets.getOrCreateOutlet(newOutlet);
        globalOutletOverrides.put((int) previousOutlet.get(), overrideOutlet);

        return OverrideResult.SUCCESS;
    }

    public List<LorebookSnapshot> getLorebooks(){
        return lorebooks;
    }
    /** @return (outletId, value) from entries */
    public Result<Record2<Integer, String>> getOutlets(){
        return lorebookContext.outlets.getOutletsFromIds(activeEntriesByOutlet.keySet());
    }
    public List<LorebookSnapshot> usedLorebooks(){
        return lorebooks;
    }
    public List<EntryRecord> activatedEntries(){
        return activeEntriesByOutlet.values().stream().flatMap(List::stream).toList();
    }

    public @NonNull Optional<List<EntryRecord>> getOf(int outletId){
        return Optional.ofNullable(activeEntriesByOutlet.get(outletId));
    }
    public boolean isActive(int lorebookId, int entryId){
        return activeEntriesIds.contains(IntIntPair.of(lorebookId, entryId));
    }

    void activateEntries(PromptRenderer renderer){
        activateEntries(renderer, lorebookContext.entryKeywords);
    }
    /**
     * TODO : Optimize this shit. Thank GOD I'm using H2 instead of going through JNI on each call
     */
    void activateEntries(PromptRenderer builder, EntryKeywordService entryKeywords){
        if (hasCheckedEntries)
            throw new IllegalStateException("Activate entries has been called twice");
        hasCheckedEntries = true;

        IntSet lorebookIds = IntSetFactory.ofValues(
                lorebooks.stream()
                        .mapToInt(rec -> rec.asReference().id())
                        .toArray()
        );
        KeywordDetector keywordDetector = new KeywordDetector(builder, lorebookIds, entryKeywords);

        int firstDetected = keywordDetector.getNumberOfDetectedKeywords();
        Result<EntryRecord> entries = lorebookContext.entries.getAllActiveEntriesOf(lorebookIds);
        Queue<Pair<EntryRecord, Set<Integer>>> recursableEntries = new ArrayDeque<>(entries.size());

        //First pass, evaluate all entries.
        List<EntryRecord> firstPassEntries = doFirstPass(entryKeywords, entries, keywordDetector, recursableEntries);
        firstPassEntries.forEach(entry -> {
            if (!entry.getPreventFurtherRecursion()) keywordDetector.addContainedIn(entry);
        });

        recursiveActivateEntries(recursableEntries, firstDetected, keywordDetector);

        // Post process
        activeEntriesByOutlet.values()
                .forEach(entryList -> entryList.sort(
                        Comparator
                                .comparingInt(EntryRecord::getLorebookId)
                                .thenComparingInt(EntryRecord::getPosition)
                                .thenComparingInt(EntryRecord::getEntryId)
                        )
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
                    case KEYWORDS_MISSING -> {
                        recursableEntries.add(entryAndKeywords);
                    }
                    case FAILED -> {}
                }
            }

            currentlyDetected = keywordDetector.getNumberOfDetectedKeywords();
        }
    }

    private void markActive(@NonNull EntryRecord entry) {
        IntIntPair entryKey = IntIntPair.of(entry.getLorebookId(), entry.getEntryId());
        if (!activeEntriesIds.add(entryKey) || !budgetManager.hasSpaceFor(entry.getContent(), TextType.LOREBOOK_ENTRY)) {
            return;
        }

        activeEntriesByOutlet
                .computeIfAbsent(getEntryOutlet(entry), k -> new ArrayList<>())
                .add(entry);
    }

    private int getEntryOutlet(@NonNull EntryRecord entry){
        IntIntPair key = IntIntPair.of(entry.getLorebookId(), entry.getOutlet());
        if (lorebookOutletOverrides.containsKey(key))
            return lorebookOutletOverrides.getInt(key);

        int lorebookId = entry.getLorebookId();
        if (lorebookOverrides.containsKey(lorebookId))
            return lorebookOverrides.get(lorebookId);

        if (globalOutletOverrides.containsKey((int) entry.getOutlet()))
            return globalOutletOverrides.get(entry.getOutlet());

        return entry.getOutlet();
    }
}