package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.microservices.EntityStore;
import chechelpo.frplm.jooq.generated.tables.Entry;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @NotNull List<EntryRecord> getEntriesWith(
            @NotNull EntityKey<LorebooksRecord> lorebookKey,
            int outletID,
            IntSet keywordIDs
    ){
        return this.ctx
                .selectFrom(ENTRY)
                .where(
                        ENTRY.OUTLET.eq(outletID)
                                .and(ENTRY.LOREBOOK_ID.eq(lorebookKey.getValue(LOREBOOKS.ID))))
                .andExists(
                        this.ctx.selectOne()
                                .from(ENTRY_KEYWORDS)
                                .where(ENTRY_KEYWORDS.LOREBOOK_ID.eq(ENTRY.LOREBOOK_ID))
                                .and(ENTRY_KEYWORDS.ENTRY_ID.eq(ENTRY.ENTRY_ID))
                                .and(ENTRY_KEYWORDS.KEYWORD_ID.in(keywordIDs))
                )
                .fetchInto(EntryRecord.class);
    }
}
