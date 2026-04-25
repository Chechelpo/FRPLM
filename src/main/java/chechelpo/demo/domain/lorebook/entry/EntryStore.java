package chechelpo.demo.domain.lorebook.entry;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.demo.jooq.generated.tables.Entry;
import chechelpo.demo.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public final class EntryStore extends ABSEntityStore<EntryRecord> {

    protected EntryStore(@NotNull DSLContext ctx) {
        super(ctx, Entry.ENTRY, EntityTypes.Types.ENTRIES);
    }
}
