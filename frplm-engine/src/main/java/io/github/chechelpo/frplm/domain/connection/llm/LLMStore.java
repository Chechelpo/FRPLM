package io.github.chechelpo.frplm.domain.connection.llm;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.domain.EntityTypes;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.LlmConnection;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

@Store
final class LLMStore extends EntityStore<LlmConnectionRecord> {
    LLMStore(@NotNull DSLContext ctx) {
        super(ctx, LlmConnection.LLM_CONNECTION, EntityTypes.Types.LLM_CONNECTION);
    }
}
