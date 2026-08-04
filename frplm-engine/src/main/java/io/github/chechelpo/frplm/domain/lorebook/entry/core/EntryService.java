package io.github.chechelpo.frplm.domain.lorebook.entry.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.FieldValidator;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.Entry;
import io.github.chechelpo.frplm.jooq.generated.tables.Lorebooks;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewEntryOrder;
import io.github.chechelpo.frplm.utils.importers.sillytavern.STLorebookImporter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jooq.Result;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.*;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Component
public class EntryService extends EntityService<EntryRecord, EntryStore> implements SmartInitializingSingleton {
    private final LorebookService lorebooks;
    private final OutletService outlets;
    private final EntryKeywordService entryKeywordService;

    EntryService(
            EntryStore entriesStore,
            FieldValidator<EntryRecord> validator,
            LorebookService lorebooks,
            OutletService outlets,
            EventBus eventBus,
            EntryKeywordService entryKeywordService
    ) {
        super(entriesStore, validator, eventBus);
        this.lorebooks = lorebooks;
        this.outlets = outlets;
        this.entryKeywordService = entryKeywordService;
    }

    @Override
    public void afterSingletonsInstantiated() {
        lorebooks.validateKeyStructure(lorebookKeyOf(1))
                .ifFailureThrow(msg -> new IllegalStateException("Entry service has wrong key building \n" + msg));
    }

    private EntityKey<LorebooksRecord> lorebookKeyOf(int lorebookId){
        return EntityKey.of(LOREBOOKS.ID, lorebookId);
    }

    public Result<EntryRecord> getAllActiveEntriesOf(IntSet lorebookIds){
        return store.getActiveOfLorebooks(lorebookIds);
    }


    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<EntryRecord> data, long operationID) {
        EntityKey<LorebooksRecord> parentLorebookKey = lorebookKeyOf(data.require(Entry.ENTRY.LOREBOOK_ID));

        int newEntryID = lorebooks.incrementAndGet(
                Lorebooks.LOREBOOKS.NEXT_ENTRY_ID,
                parentLorebookKey
        ).orElseThrow(() -> {
            log.error("Could not fetch entryID creating entry\n Assignments: {}", data.assignments());
            return new UnexpectedException("Could not fetch new entry ID", Severity.SYSTEM);
        });
        int defaultOutletID = lorebooks.getValueOf(LOREBOOKS.DEFAULT_OUTLET_ID, parentLorebookKey)
                .orElseThrow(() -> new UnexpectedException("Could not fetch defaultOutletID from parent", Severity.SYSTEM));

        data.set(Entry.ENTRY.ENTRY_ID, newEntryID);
        data.set(ENTRY.OUTLET, defaultOutletID);

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void beforeUpdate(@NotNull EntityKey<EntryRecord> target, @NotNull EntityDataPayload<EntryRecord> data, long operationID) {
        if (data.assigns(Entry.ENTRY.OUTLET) && data.require(Entry.ENTRY.OUTLET) != null)
            throw new InvalidValue("Cannot update an outlet entry through normal outlet. Use updateOutlet() instead.");
        super.beforeUpdate(target, data, operationID);
    }

    public int updateOutlet(EntityKey<EntryRecord> id, String newOutletName) {
        int outletId = outlets.getOrCreateOutlet(newOutletName);
        store.update(
                id,
                EntityDataPayload.of(
                        ENTRY.OUTLET,
                        outletId
                )
        );
        return outletId;
    }
    @Transactional
    public EntryRecord exchangeEntry(EntityKey<EntryRecord> entryKey, int toLorebookId){
        Objects.requireNonNull(entryKey, "Entry key is null");
        EntryRecord entry = this.find(entryKey).orElseThrow(notFound -> {
            log.error("No such entry with key: {} \n when exchanging lorebooks", entryKey);
            return new EntityNotFound("No such entry with key " + entryKey.toString(), Severity.SYSTEM);
        });
        LorebooksRecord newLorebook = lorebooks.find(lorebookKeyOf(toLorebookId)).orElseThrow(notFound -> {
            log.error("No such destination lorebook with id {} when exchanging entry: {}", toLorebookId, entry.getName());
            return new EntityNotFound("No such lorebook with id " + toLorebookId, Severity.SYSTEM);
        });
        if (entry.getLorebookId() == toLorebookId) throw new InvalidValue("Tried to change to same lorebook");

        Set<String> keywords = entryKeywordService.keywordsOfEntry(entry.getLorebookId(), entry.getEntryId());

        EntityDataPayload<EntryRecord> relevantData = getRelevantData(entry, newLorebook);
        relevantData.set(ENTRY.LOREBOOK_ID, toLorebookId);

        boolean deleted = this.delete(entryKey);
        if (!deleted) throw new UnexpectedException("Couldn't delete old entry on exchange", Severity.SYSTEM);

        EntryRecord newEntry = this.createAndGet(relevantData);
        keywords.forEach(key -> entryKeywordService.associate(newEntry.getLorebookId(), newEntry.getEntryId(), key));

        return newEntry;
    }

    private @NonNull EntityDataPayload<EntryRecord> getRelevantData(EntryRecord entry, LorebooksRecord newLorebook) {
        return EntityDataPayload.<EntryRecord>builder()
                .set(ENTRY.NAME, entry.getName())
                .set(ENTRY.CONTENT, entry.getContent())
                .set(ENTRY.OUTLET, lorebooks.getValueOf(LOREBOOKS.DEFAULT_OUTLET_ID, lorebooks.keyOf(newLorebook)).orElseThrow())

                .set(ENTRY.ENABLED, entry.getEnabled())
                .set(ENTRY.PROBABILITY, entry.getProbability())
                .set(ENTRY.DELAY, entry.getDelay())
                .set(ENTRY.COOLDOWN, entry.getCooldown())
                .set(ENTRY.STICK_THROUGH, entry.getStickThrough())
                .set(ENTRY.POSITION, entry.getPosition())
                .set(ENTRY.STRATEGY, entry.getStrategy())
                .set(ENTRY.PREVENT_FURTHER_RECURSION, entry.getPreventFurtherRecursion())
                .set(ENTRY.NON_RECURSABLE, entry.getNonRecursable())
                .set(ENTRY.SCAN_DEPTH, entry.getScanDepth())
                .build();
    }

    /**
     * @param lorebookIDs to query
     * @param keywordIDs to detect.
     * @return currently enabled entries of these lorebooks that contain ALL the keywordIDs (Logical AND of keywords = return)
     */
    public @NotNull Int2ObjectMap<List<EntryRecord>> getByOutletsWith(
            IntSet lorebookIDs,
            Set<Integer> keywordIDs
    ) {
        Int2ObjectMap<List<EntryRecord>> map = new Int2ObjectOpenHashMap<>(lorebookIDs.size());
        store.getEntriesWith(lorebookIDs, keywordIDs).forEach(entry -> {
            int outletID = entry.getOutlet();

            List<EntryRecord> entries = map.get(outletID);

            if (!map.containsKey(outletID)) {
                entries = new ArrayList<>();
                map.put(outletID, entries);
            }

            entries.add(entry);
        });
        return map;
    }

    /**
     * @param lorebookIDs to query
     * @param keywordIDs to detect.
     * @return currently enabled entries of these lorebooks that contain ALL the keywordIDs (Logical AND of keywords = return)
     */
    public @NotNull List<EntryRecord> getEntriesWith(
            IntSet lorebookIDs,
            Set<Integer> keywordIDs
    ) {
        return store.getEntriesWith(lorebookIDs, keywordIDs);
    }
    /**
     * @param toLorebookID lorebook to import this JSON entries to
     * @param file JSON with entries
     * @return created records
     */
    @Transactional
    public List<EntryRecord> importEntriesFromJSON(final int toLorebookID, JsonNode file){
        Objects.requireNonNull(file);
        List<NewEntryOrder> order = STLorebookImporter.getEntries(file);
        List<EntryRecord> result = new ArrayList<>(order.size());

        order.forEach(entry -> {
            EntityDataPayload<EntryRecord> payload = entry.entryInfo();
            payload.set(ENTRY.LOREBOOK_ID, toLorebookID);
            try{
                EntryRecord record = this.createAndGet(entry.entryInfo());

                entry.keywords().forEach(keyword ->
                        entryKeywordService.associate(toLorebookID, record.getEntryId(), keyword)
                );
                result.add(record);
            } catch (RuntimeException e) {
                log.error("Error importing entry {} with info \n {}", e, entry.entryInfo());
            }
        });

        return result;
    }
}
