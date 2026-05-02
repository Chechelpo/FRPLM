package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public final class LorebookStore extends ABSEntityStore<LorebooksRecord> {
    protected LorebookStore(@NotNull DSLContext ctx) {
        super(ctx, Lorebooks.LOREBOOKS, EntityTypes.Types.LOREBOOKS);
    }
}
