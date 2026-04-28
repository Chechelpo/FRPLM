package chechelpo.demo.domain.lorebook.entry;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.demo.jooq.generated.tables.Entry;
import chechelpo.demo.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class EntryStore extends ABSEntityStore<EntryRecord> {

    EntryStore(@NotNull DSLContext ctx) {
        super(ctx, Entry.ENTRY, EntityTypes.Types.ENTRIES);
    }

    @NotNull List<EntryRecord> getOfLorebook(Integer LorebookID){
        return this.ctx.selectFrom(Entry.ENTRY)
                .where(Entry.ENTRY.LOREBOOK_ID.eq(LorebookID))
                .fetch();
    }
}
