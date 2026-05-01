package chechelpo.demo.domain.lorebook.core;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.demo.jooq.generated.tables.Lorebooks;
import chechelpo.demo.jooq.generated.tables.records.LorebooksRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public final class LorebookStore extends ABSEntityStore<LorebooksRecord> {
    protected LorebookStore(@NotNull DSLContext ctx) {
        super(ctx, Lorebooks.LOREBOOKS, EntityTypes.Types.LOREBOOKS);
    }
}
