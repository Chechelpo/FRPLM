package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.Entry;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.*;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
public final class EntryStore extends EntityStore<EntryRecord> {

    EntryStore(@NotNull DSLContext ctx) {
        super(ctx, Entry.ENTRY, EntityTypes.Types.ENTRIES);
    }

    @NotNull List<EntryRecord> getOfLorebook(Integer LorebookID){
        return this.ctx.selectFrom(Entry.ENTRY)
                .where(Entry.ENTRY.LOREBOOK_ID.eq(LorebookID))
                .fetch();
    }

    /**
     * @param lorebookIDs ids of lorebooks
     * @param keywordIDs keywords detected
     * @return map < outletID -> list of entries to inject >
     */
    @NotNull
    Int2ObjectMap<List<EntryRecord>> getEntriesByOutletWith(
            @NotNull IntSet lorebookIDs,
            @NotNull IntSet keywordIDs
    ) {
        Int2ObjectMap<List<EntryRecord>> map =
                new Int2ObjectOpenHashMap<>(lorebookIDs.size());

        map.defaultReturnValue(Collections.emptyList());

        if (lorebookIDs.isEmpty() || keywordIDs.isEmpty()) {
            return map;
        }

        this.ctx
                .selectFrom(ENTRY)
                .where(ENTRY.LOREBOOK_ID.in(lorebookIDs))

                // Entry must have at least one keyword.
                .andExists(
                        ctx.selectOne()
                                .from(ENTRY_KEYWORDS)
                                .where(ENTRY_KEYWORDS.LOREBOOK_ID.eq(ENTRY.LOREBOOK_ID))
                                .and(ENTRY_KEYWORDS.ENTRY_ID.eq(ENTRY.ENTRY_ID))
                )

                // No keyword attached to this entry may be absent from detected keywordIDs.
                .andNotExists(
                        ctx.selectOne()
                                .from(ENTRY_KEYWORDS)
                                .where(ENTRY_KEYWORDS.LOREBOOK_ID.eq(ENTRY.LOREBOOK_ID))
                                .and(ENTRY_KEYWORDS.ENTRY_ID.eq(ENTRY.ENTRY_ID))
                                .and(ENTRY_KEYWORDS.KEYWORD_ID.notIn(keywordIDs))
                )

                .fetch()
                .forEach(entry -> {
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
}
