package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.LlmGenRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.LLM_GEN;

@Component
final class GenStore extends EntityStore<LlmGenRecord> {
    GenStore(@NotNull DSLContext ctx) {
        super(ctx, LLM_GEN, EntityTypes.Types.LLM_GEN);
    }
}
