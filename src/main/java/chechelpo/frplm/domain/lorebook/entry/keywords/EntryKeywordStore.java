package chechelpo.frplm.domain.lorebook.entry.keywords;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityStore;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class EntryKeywordStore extends EntityStore<EntryKeywordsRecord> {
    public EntryKeywordStore(@NotNull DSLContext ctx) {
        super(ctx, ENTRY_KEYWORDS, EntityTypes.Types.ENTRY_KEYWORDS);
    }

    public @NotNull List<KeywordRecord> getOfEntry(EntityKey<EntryRecord> key){
        return ctx.select()
                .from(ENTRY)
                .join(ENTRY_KEYWORDS)
                .on(
                        ENTRY.LOREBOOK_ID.eq(ENTRY_KEYWORDS.LOREBOOK_ID)
                                .and(ENTRY.ENTRY_ID.eq(ENTRY_KEYWORDS.ENTRY_ID))
                                .and(key.getPkCondition())
                )
                .join(KEYWORD)
                .on(KEYWORD.ID.eq(ENTRY_KEYWORDS.KEYWORD_ID))
                .fetchInto(KeywordRecord.class);
    }

    public IntSet getKeywordIDsOfLorebook(@NotNull EntityKey<LorebooksRecord> key){
        List<Integer> keywordIDs = ctx.selectDistinct(ENTRY_KEYWORDS)
                .where(ENTRY_KEYWORDS.LOREBOOK_ID.eq(key.getValue(LOREBOOKS.ID)))
                .fetch(ENTRY_KEYWORDS.KEYWORD_ID);
        return IntSetFactory.ofValues(keywordIDs);
    }
}
