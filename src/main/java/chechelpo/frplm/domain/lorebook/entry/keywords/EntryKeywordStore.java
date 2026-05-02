package chechelpo.frplm.domain.lorebook.entry.keywords;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class EntryKeywordStore extends ABSEntityStore<EntryKeywordsRecord> {
    public EntryKeywordStore(@NotNull DSLContext ctx) {
        super(ctx, ENTRY_KEYWORDS, EntityTypes.Types.ENTRY_KEYWORDS);
    }

    public @NotNull List<KeywordRecord> getByEntry(EntityKey<EntryRecord> key){
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
}
