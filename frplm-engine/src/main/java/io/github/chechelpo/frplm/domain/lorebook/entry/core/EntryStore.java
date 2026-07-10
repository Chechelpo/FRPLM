package io.github.chechelpo.frplm.domain.lorebook.entry.core;

import io.github.chechelpo.frplm.domain.EntityTypes;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import chechelpo.frplm.jooq.generated.tables.Entry;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.springframework.stereotype.Component;

import java.util.*;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
public final class EntryStore extends EntityStore<EntryRecord> {

    EntryStore(@NotNull DSLContext ctx) {
        super(ctx, Entry.ENTRY, EntityTypes.Types.ENTRIES);
    }

    public Result<EntryRecord> getActiveOfLorebooks(IntSet lorebookIds){
        return ctx.selectFrom(main_table)
                .where(
                        ENTRY.LOREBOOK_ID.in(lorebookIds)
                        .and(ENTRY.ENABLED.eq(true))
                )
                .fetch();
    }

    /**
     * Always returns constant entries.
     * @param lorebookIDs ids of lorebooks
     * @param keywordIDs keywords detected
     * @return list of enabled entry records with lorebook IDs and all keywords.
     */
    @NotNull
    List<EntryRecord> getEntriesWith(
            @NotNull IntSet lorebookIDs,
            @NotNull Set<Integer> keywordIDs
    ) {
        return this.ctx
                .selectFrom(ENTRY)
                .where(ENTRY.LOREBOOK_ID.in(lorebookIDs).and(ENTRY.ENABLED))
                .and(
                        ENTRY.STRATEGY.eq(ActivationStrategy.CONSTANT.stable_id)
                                .orExists(
                                        ctx.selectOne()
                                                .from(ENTRY_KEYWORDS)
                                                .where(ENTRY_KEYWORDS.LOREBOOK_ID.eq(ENTRY.LOREBOOK_ID))
                                                .and(ENTRY_KEYWORDS.ENTRY_ID.eq(ENTRY.ENTRY_ID))
                                )
                )
                .fetch();
    }
    /**
     * @param lorebookIDs ids of lorebooks
     * @param keywordIDs keywords detected
     * @return list of enabled entry records
     */
    @NotNull
    List<EntryRecord> getEntriesWith(
            @NotNull IntSet lorebookIDs,
            @NotNull IntSet alreadySeenIDs,
            @NotNull Set<Integer> keywordIDs
    ) {
        return this.ctx
                .selectFrom(ENTRY)
                .where(ENTRY.LOREBOOK_ID.in(lorebookIDs)
                        .and(ENTRY.ENABLED)
                        .and(ENTRY.ENTRY_ID.notIn(alreadySeenIDs))
                )
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
                .fetch();
    }

    boolean move(int fromLorebookId, int toLorebookId, int entryId){
        return this.ctx.update(ENTRY)
                .set(ENTRY.LOREBOOK_ID, toLorebookId)
                .where(
                        ENTRY.LOREBOOK_ID.eq(fromLorebookId)
                                .and(ENTRY.ENTRY_ID.eq(entryId))
                )
                .execute() == 1;
    }
}
