package io.github.chechelpo.frplm.domain.lorebook.entry.keywords;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class EntryKeywordStore extends EntityStore<EntryKeywordsRecord> {
    public EntryKeywordStore(@NotNull DSLContext ctx) {
        super(ctx, ENTRY_KEYWORDS, EntityConfigs.Types.ENTRY_KEYWORDS);
    }

    public @NotNull Set<String> getOfEntry(int lorebookId, int entryId) {
        return ctx.select()
                .from(ENTRY)
                .join(ENTRY_KEYWORDS)
                .on(
                        ENTRY.LOREBOOK_ID.eq(ENTRY_KEYWORDS.LOREBOOK_ID)
                                .and(ENTRY.ENTRY_ID.eq(ENTRY_KEYWORDS.ENTRY_ID))
                )
                .join(KEYWORD)
                .on(KEYWORD.ID.eq(ENTRY_KEYWORDS.KEYWORD_ID))
                .where(ENTRY.LOREBOOK_ID.eq(lorebookId)
                        .and(ENTRY.ENTRY_ID.eq(entryId)))
                .fetchSet(KEYWORD.KEYWORD_);
    }
    public @NotNull Set<Integer> getIdsOfEntry(int lorebookId, int entryId) {
        return ctx.select()
                .from(ENTRY)
                .join(ENTRY_KEYWORDS)
                .on(
                        ENTRY.LOREBOOK_ID.eq(ENTRY_KEYWORDS.LOREBOOK_ID)
                                .and(ENTRY.ENTRY_ID.eq(ENTRY_KEYWORDS.ENTRY_ID))
                )
                .join(KEYWORD)
                .on(KEYWORD.ID.eq(ENTRY_KEYWORDS.KEYWORD_ID))
                .where(ENTRY.LOREBOOK_ID.eq(lorebookId)
                        .and(ENTRY.ENTRY_ID.eq(entryId)))
                .fetchSet(KEYWORD.ID);
    }

    public IntSet getKeywordIDsOfLorebook(@NotNull EntityKey<LorebooksRecord> key){
        List<Integer> keywordIDs = ctx.selectDistinct(ENTRY_KEYWORDS)
                .where(ENTRY_KEYWORDS.LOREBOOK_ID.eq(key.getValue(LOREBOOKS.ID)))
                .fetch(ENTRY_KEYWORDS.KEYWORD_ID);
        return IntSetFactory.ofValues(keywordIDs);
    }

    public @NotNull List<IntObjectPair<String>> getKeywordsOf(IntSet lorebookIDs) {
        return ctx.selectDistinct(KEYWORD.KEYWORD_, KEYWORD.ID)
                .from(KEYWORD)
                .join(ENTRY_KEYWORDS)
                .on(KEYWORD.ID.eq(ENTRY_KEYWORDS.KEYWORD_ID))
                .join(ENTRY)
                .on(
                        ENTRY_KEYWORDS.LOREBOOK_ID.eq(ENTRY.LOREBOOK_ID)
                                .and(ENTRY_KEYWORDS.ENTRY_ID.eq(ENTRY.ENTRY_ID))
                )
                .where(ENTRY_KEYWORDS.LOREBOOK_ID.in(lorebookIDs))
                .fetch(record -> IntObjectPair.of(record.get(KEYWORD.ID), record.get(KEYWORD.KEYWORD_)));
    }

    public @NotNull Set<String> getKeywordNamesOfLorebook(int lorebookID){
        return ctx.selectDistinct(KEYWORD.KEYWORD_)
                .from(KEYWORD)
                .join(ENTRY_KEYWORDS)
                .on(
                        KEYWORD.ID.eq(ENTRY_KEYWORDS.KEYWORD_ID)
                                .and(ENTRY_KEYWORDS.LOREBOOK_ID.eq(lorebookID))
                )
                .where(ENTRY_KEYWORDS.LOREBOOK_ID.eq(lorebookID))
                .fetchSet(KEYWORD.KEYWORD_);
    }
}
